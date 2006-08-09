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

	private long _readTimeout = 3000;

	public Resource getResource(String source) throws IOException {
		return lslToResource(source, sendCommand("ls -l "+source, true));
	}

	public void get(String source, File destination) throws IOException {
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
		
		sendCommand("get "+source, getExpectedDownloadMessage(source, to));
		
		to.renameTo(destination);
	}

	public List list(String parent) throws IOException {
		if (!parent.endsWith("/")) {
			parent = parent+"/";
		}
		String response = sendCommand("ls -l "+parent, true);
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
	}

	public void put(File source, String destination, boolean overwrite) throws IOException {
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
		sendCommand("put "+source.getAbsolutePath(), getExpectedUploadMessage(source, to));
		sendCommand("mv "+to+" "+destination);
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
		return sendCommand(command, false);
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
	protected void sendCommand(String command, Pattern expectedResponse) throws IOException {
		String response = sendCommand(command, true);
		if (!expectedResponse.matcher(response).matches()) {
			Message.debug("invalid response from server:");
			Message.debug("expected: '"+expectedResponse+"'");
			Message.debug("was:      '"+response+"'");
			throw new IOException(response);
		}
	}
	protected String sendCommand(String command, boolean sendErrorAsResponse) throws IOException {
		ensureConnectionOpened();
		Message.debug("sending command '"+command+"' to "+getHost());
		_out.println(command);
		_out.flush();
		
		return readResponse(sendErrorAsResponse);
	}

	protected String readResponse(final boolean sendErrorAsResponse) throws IOException {
		final StringBuffer response = new StringBuffer();
		final IOException[] exc = new IOException[1];
		final boolean[] done = new boolean[1];
		Thread reader = new Thread() {
			public void run() {
				try {
					int c;
					while ((c = _in.read()) != -1) {
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
			reader.join(_readTimeout);
		} catch (InterruptedException e) {
		}
		if (exc[0] != null) {
			throw exc[0];
		} else if (!done[0]) {
			throw new IOException("connection timeout to "+getHost());
		} else {
			Message.debug("received response '"+response+"' from "+getHost());
			return response.toString();
		}
	}

	protected synchronized void ensureConnectionOpened() throws IOException {
		if (_in == null) {
			Message.verbose("connecting to "+getUsername()+"@"+getHost()+"... ");
			String connectionCommand = getConnectionCommand();
			Message.debug("launching '"+connectionCommand+"'");
			Process p = Runtime.getRuntime().exec(connectionCommand);
			_in = new InputStreamReader(p.getInputStream());
			_err = new InputStreamReader(p.getErrorStream());
			_out = new PrintWriter(p.getOutputStream());
			
			Ivy ivy = IvyContext.getContext().getIvy();
			ivy.addIvyListener(new IvyListener() {
				public void progress(IvyEvent event) {
					disconnect();
					event.getSource().removeIvyListener(this);
				}
			}, EndResolveEvent.NAME);
			
			new Thread("err-stream-reader") {
				public void run() {
					int c;
					try {
						while ((c = _err.read()) != -1) {
							_errors.append((char)c);
						}
					} catch (IOException e) {
					}
				}
			}.start();
			try {
				readResponse(false); // waits for first prompt
			} catch (IOException ex) {
				closeConnection();
				throw new IOException("impossible to connect to "+getUsername()+"@"+getHost()+" using "+getAuthentication()+": "+ex.getMessage());
			}
			Message.verbose("connected to "+getHost());
		}
	}

	public synchronized void disconnect() {
		if (_in != null) {
			Message.verbose("disconnecting from "+getHost()+"... ");
			try {
				sendCommand("exit");
			} catch (IOException e) {
			} finally {
				closeConnection();
				Message.verbose("disconnected of "+getHost());
			}
		}
	}

	private void closeConnection() {
		try {
			_in.close();
		} catch (IOException e) {
		}
		try {
			_err.close();
		} catch (IOException e) {
		}
		_out.close();
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
}
