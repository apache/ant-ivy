package fr.jayasoft.ivy.repository.vsftp;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.IvyContext;
import fr.jayasoft.ivy.event.IvyEvent;
import fr.jayasoft.ivy.event.IvyListener;
import fr.jayasoft.ivy.event.resolve.EndResolveEvent;
import fr.jayasoft.ivy.repository.AbstractRepository;
import fr.jayasoft.ivy.repository.Resource;
import fr.jayasoft.ivy.util.Message;

/**
 * Repository using SecureCRT vsftp command line program to access an sftp repository
 * 
 * This is especially useful to leverage the gssapi authentication supported by SecureCRT.
 * 
 * In caseswhere usual sftp is enough, prefer the 100% java solution of sftp repository.
 * 
 * This requires SecureCRT to be in the PATH.
 * 
 * Tested with SecureCRT 5.0.5
 * 
 * @author Xavier Hanin
 *
 */
public class VsftpRepository extends AbstractRepository {
	private static final String PROMPT = "vsftp> ";

	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US);
	
	private String _host;
	private String _username;
	private String _authentication = "gssapi";
	
	private Reader _in;
	private Reader _err;
	private PrintWriter _out;
	
	private volatile StringBuffer _errors = new StringBuffer();

	private long _readTimeout = 10000;
	
	private long _reuseConnection = 5 * 60 * 1000; // reuse connection during 5 minutes by default

	private volatile long _lastCommand;

	private volatile boolean _inCommand;

	private Process _process;

	private Thread _connectionCleaner;

	private Thread _errorsReader;

	public Resource getResource(String source) throws IOException {
		try {
			return lslToResource(source, sendCommand("ls -l "+source, true, true));
		} catch (IOException ex) {
			cleanup(ex);
			throw ex;
		} finally {
			cleanup();
		}
	}

	public void get(String source, File destination) throws IOException {
		try {
			File destDir = destination.getParentFile();
			if (destDir != null) {
				sendCommand("lcd "+destDir.getAbsolutePath());
			}
			if (destination.exists()) {
				destination.delete();
			}

			int index = source.lastIndexOf('/');
			String srcName = index == -1?source:source.substring(index+1);
			File to = destDir == null ? new File(srcName):new File(destDir, srcName);

			sendCommand("get "+source, getExpectedDownloadMessage(source, to), 0);

			to.renameTo(destination);
		} catch (IOException ex) {
			cleanup(ex);
			throw ex;
		} finally {
			cleanup();
		}
	}

	public List list(String parent) throws IOException {
		try {
			if (!parent.endsWith("/")) {
				parent = parent+"/";
			}
			String response = sendCommand("ls -l "+parent, true, true);
			if (response.startsWith("ls")) {
				return null;
			}
			String[] lines = response.split("\n");
			List ret = new ArrayList(lines.length);
			for (int i = 0; i < lines.length; i++) {
				while (lines[i].endsWith("\r") || lines[i].endsWith("\n")) {
					lines[i] = lines[i].substring(0, lines[i].length() -1);
				}
				if (lines[i].trim().length() != 0) {
					ret.add(parent+lines[i].substring(lines[i].lastIndexOf(' ')+1));
				}
			}
			return ret;
		} catch (IOException ex) {
			cleanup(ex);
			throw ex;
		} finally {
			cleanup();
		}
	}

	public void put(File source, String destination, boolean overwrite) throws IOException {
		try {
			if (getResource(destination).exists()) {
				if (overwrite) {
					sendCommand("rm "+destination, getExpectedRemoveMessage(destination));
				} else {
					return;
				}
			}
			int index = destination.lastIndexOf('/');
			String destDir = null; 
			if (index != -1) {
				destDir = destination.substring(0, index);
				mkdirs(destDir);
				sendCommand("cd "+destDir);
			}
			String to = destDir != null ? destDir+"/"+source.getName():source.getName();
			sendCommand("put "+source.getAbsolutePath(), getExpectedUploadMessage(source, to), 0);
			sendCommand("mv "+to+" "+destination);
		} catch (IOException ex) {
			cleanup(ex);
			throw ex;
		} finally {
			cleanup();
		}
	}


	private void mkdirs(String destDir) throws IOException {
		if (dirExists(destDir)) {
			return;
		}
		if (destDir.endsWith("/")) {
			destDir = destDir.substring(0, destDir.length() - 1);
		}
		int index = destDir.lastIndexOf('/');
		if (index != - 1) {
			mkdirs(destDir.substring(0, index));;
		}
		sendCommand("mkdir "+destDir);
	}

	private boolean dirExists(String dir) throws IOException {
		return !sendCommand("ls "+dir, true).startsWith("ls: ");
	}

	protected String sendCommand(String command) throws IOException {
		return sendCommand(command, false, _readTimeout);
	}
	
	protected void sendCommand(String command, Pattern expectedResponse) throws IOException {
		sendCommand(command, expectedResponse, _readTimeout);
	}

	/**
	 * The behaviour of vsftp with some commands is to log the resulting message on the error stream,
	 * even if everything is ok.
	 *  
	 * So it's quite difficult if there was an error or not.
	 * 
	 * Hence we compare the response with the expected message and deal with it.
	 * The problem is that this is very specific to the version of vsftp used for the test,
	 * 
	 * That's why expected messages are obtained using overridable protected methods.
	 */ 
	protected void sendCommand(String command, Pattern expectedResponse, long timeout) throws IOException {
		String response = sendCommand(command, true, timeout);
		if (!expectedResponse.matcher(response).matches()) {
			Message.debug("invalid response from server:");
			Message.debug("expected: '"+expectedResponse+"'");
			Message.debug("was:      '"+response+"'");
			throw new IOException(response);
		}
	}
	protected String sendCommand(String command, boolean sendErrorAsResponse) throws IOException {
		return sendCommand(command, sendErrorAsResponse, _readTimeout);
	}

	protected String sendCommand(String command, boolean sendErrorAsResponse, boolean single) throws IOException {
		return sendCommand(command, sendErrorAsResponse, single, _readTimeout);
	}

	protected String sendCommand(String command, boolean sendErrorAsResponse, long timeout) throws IOException {
		return sendCommand(command, sendErrorAsResponse, false, timeout);
	}
	
	protected String sendCommand(String command, boolean sendErrorAsResponse, boolean single, long timeout) throws IOException {
		single = false; // use of alone commands does not work properly due to a long delay between end of process and end of stream... 

		IvyContext.getContext().getIvy().checkInterrupted();
		_inCommand = true;
		if (!single || _in != null) {
			ensureConnectionOpened();
			Message.debug("sending command '"+command+"' to "+getHost());
			updateLastCommandTime();
			_out.println(command);
			_out.flush();
		} else {
			sendSingleCommand(command);
		}
		
		try {
			return readResponse(sendErrorAsResponse, timeout);
		} finally {
			_inCommand = false;
			if (single) {
				closeConnection();
			}
		}
	}

	protected String readResponse(boolean sendErrorAsResponse) throws IOException {
		return readResponse(sendErrorAsResponse, _readTimeout);
	}

	protected String readResponse(final boolean sendErrorAsResponse, long timeout) throws IOException {
		final StringBuffer response = new StringBuffer();
		final IOException[] exc = new IOException[1];
		final boolean[] done = new boolean[1];
		Thread reader = new Thread() {
			public void run() {
				try {
					int c;
					while ((c = _in.read()) != -1) {
						System.out.print((char)c);
						response.append((char)c);
						if (response.length() >= PROMPT.length() 
								&& response.substring(response.length() - PROMPT.length(), response.length()).equals(PROMPT)) {
							response.setLength(response.length() - PROMPT.length());
							break;
						}
					}
					if (_errors.length() > 0) {
						if (sendErrorAsResponse) {
							response.append(_errors);
							_errors.setLength(0);
						} else {
							throw new IOException(chomp(_errors).toString());
						}
					}
					chomp(response);
					done[0] = true;
				} catch (IOException e) {
					exc[0]  = e;
				}			
			}
		};
		reader.start();
		try {
			reader.join(timeout);
		} catch (InterruptedException e) {
		}
		updateLastCommandTime();
		if (exc[0] != null) {
			throw exc[0];
		} else if (!done[0]) {
			if (reader.isAlive())  {
				reader.stop(); // no way to interrupt it non abruptly
			}
			throw new IOException("connection timeout to "+getHost());
		} else {
			if ("Not connected.".equals(response)) {
				Message.info("vsftp connection to "+getHost()+" reset");
				closeConnection();
				throw new IOException("not connected to "+getHost());
			}
			Message.debug("received response '"+response+"' from "+getHost());
			return response.toString();
		}
	}

	private synchronized void sendSingleCommand(String command) throws IOException {
		exec(getSingleCommand(command));
	}

	protected synchronized void ensureConnectionOpened() throws IOException {
		if (_in == null) {
			Message.verbose("connecting to "+getUsername()+"@"+getHost()+"... ");
			String connectionCommand = getConnectionCommand();
			exec(connectionCommand);

			try {
				readResponse(false); // waits for first prompt

				if (_reuseConnection > 0) {
					_connectionCleaner = new Thread() {
						public void run() {
							try {
								long sleep = 10;
								while (_in != null && sleep > 0) {
									sleep(sleep);
									sleep = _reuseConnection - (System.currentTimeMillis() - _lastCommand);
									if (_inCommand) {
										sleep = sleep <= 0 ? _reuseConnection : sleep;
									}
								}
							} catch (InterruptedException e) {
							}
							disconnect();
						}
					};
					_connectionCleaner.start();
				}

				Ivy ivy = IvyContext.getContext().getIvy();
				ivy.addIvyListener(new IvyListener() {
					public void progress(IvyEvent event) {
						disconnect();
						event.getSource().removeIvyListener(this);
					}
				}, EndResolveEvent.NAME);
				
				
			} catch (IOException ex) {
				closeConnection();
				throw new IOException("impossible to connect to "+getUsername()+"@"+getHost()+" using "+getAuthentication()+": "+ex.getMessage());
			}
			Message.verbose("connected to "+getHost());
		}
	}

	private void updateLastCommandTime() {
		_lastCommand = System.currentTimeMillis();
	}

	private void exec(String command) throws IOException {
		Message.debug("launching '"+command+"'");
		_process = Runtime.getRuntime().exec(command);
		_in = new InputStreamReader(_process.getInputStream());
		_err = new InputStreamReader(_process.getErrorStream());
		_out = new PrintWriter(_process.getOutputStream());
		
		_errorsReader = new Thread() {
							public void run() {
								int c;
								try {
									while (_err != null && (c = _err.read()) != -1) {
										_errors.append((char)c);
									}
								} catch (IOException e) {
								}
							}
						};
		_errorsReader.start();
	}


	/**
	 * Called whenever an api level method end
	 */
	private void cleanup(Exception ex) {
		if (ex.getMessage().equals("connection timeout to "+getHost())) {
			closeConnection();
		} else {
			disconnect();
		}
	}

	/**
	 * Called whenever an api level method end
	 */
	private void cleanup() {
		if (_reuseConnection == 0) {
			disconnect();
		}
	}
	
	public synchronized void disconnect() {
		if (_in != null) {
			Message.verbose("disconnecting from "+getHost()+"... ");
			try {
				sendCommand("exit", false, 300);
			} catch (IOException e) {
			} finally {
				closeConnection();
				Message.verbose("disconnected of "+getHost());
			}
		}
	}

	private synchronized void closeConnection() {
		if (_connectionCleaner != null) {
			_connectionCleaner.interrupt();
		}
		if (_errorsReader != null) {
			_errorsReader.interrupt();
		}
		try {
			_process.destroy();
		} catch (Exception ex) {}
		try {
			_in.close();
		} catch (Exception e) {}
		try {
			_err.close();
		} catch (Exception e) {}
		try {
			_out.close();
		} catch (Exception e) {}
		
		_connectionCleaner = null;
		_errorsReader = null;
		_process = null;
		_in = null;
		_out = null;
		_err = null;
	}

	/**
	 * Parses a ls -l line and transforms it in a resource
	 * @param file
	 * @param responseLine
	 * @return
	 */
	protected Resource lslToResource(String file, String responseLine) {
		if (responseLine == null || responseLine.startsWith("ls")) {
			return new VsftpResource(this, file, false, 0, 0);
		} else {
			String[] parts = responseLine.split("\\s+");
			if (parts.length != 9) {
				Message.debug("unrecognized ls format: "+responseLine);
				return new VsftpResource(this, file, false, 0, 0);
			} else {
				try {
					long contentLength = Long.parseLong(parts[3]);
					String date = parts[4]+" "+parts[5]+" "+parts[6]+" "+parts[7];
					return new VsftpResource(this, file, true, contentLength, FORMAT.parse(date).getTime());
				} catch (Exception ex) {
					Message.warn("impossible to parse server response: "+responseLine+": "+ex);
					return new VsftpResource(this, file, false, 0, 0);
				}
			}
		}
	}

	protected String getSingleCommand(String command) {
		return "vsh -noprompt -auth "+_authentication+" "+_username+"@"+_host+" "+command;
	}
	
	protected String getConnectionCommand() {
		return "vsftp -noprompt -auth "+_authentication+" "+_username+"@"+_host;
	}
	
	protected Pattern getExpectedDownloadMessage(String source, File to) {
		return Pattern.compile("Downloading "+to.getName()+" from [^\\s]+");
	}

	protected Pattern getExpectedRemoveMessage(String destination) {
		return Pattern.compile("Removing [^\\s]+");
	}

	protected Pattern getExpectedUploadMessage(File source, String to) {
		return Pattern.compile("Uploading "+source.getName()+" to [^\\s]+");
	}


	public String getAuthentication() {
		return _authentication;
	}

	public void setAuthentication(String authentication) {
		_authentication = authentication;
	}

	public String getHost() {
		return _host;
	}

	public void setHost(String host) {
		_host = host;
	}

	public String getUsername() {
		return _username;
	}

	public void setUsername(String username) {
		_username = username;
	}

	private static StringBuffer chomp(StringBuffer str) {
		if (str == null || str.length() == 0) {
			return str;
		}
		while ("\n".equals(str.substring(str.length() - 1)) || "\r".equals(str.substring(str.length() - 1))) {
			str.setLength(str.length() - 1);
		}
		return str;
	}

	public String toString() {
		return getName()+" "+getUsername()+"@"+getHost()+" ("+getAuthentication()+")";
	}

	/**
	 * Sets the reuse connection time.
	 * The same connection will be reused if the time here does not last 
	 * between two commands.
	 * O indicates that the connection should never be reused
	 * 
	 * @param time
	 */
	public void setReuseConnection(long time) {
		_reuseConnection = time;
	}

	public long getReadTimeout() {
		return _readTimeout;
	}

	public void setReadTimeout(long readTimeout) {
		_readTimeout = readTimeout;
	}
}
