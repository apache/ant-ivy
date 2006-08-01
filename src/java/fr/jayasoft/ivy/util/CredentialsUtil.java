package fr.jayasoft.ivy.util;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import fr.jayasoft.ivy.Ivy;

public class CredentialsUtil {
	
	

	private static final class CredentialPanel extends JPanel {
		JTextField userNameField = new JTextField(20);

		JTextField passwordField = new JPasswordField(20);

		JCheckBox rememberDataCB = new JCheckBox("remember my information");
		
		CredentialPanel(Credentials credentials, File passfile) {
			GridBagLayout layout = new GridBagLayout();
			setLayout(layout);
			GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets(2,2,2,2);
			
			c.gridx = 1;
			c.gridheight = 1;
			c.gridwidth = 2;
			String prompt = credentials.getRealm() != null ? 
					"Enter username and password for \""+credentials.getRealm()+"\" at "+credentials.getHost() :
						"Enter username and password for "+credentials.getHost();
			add(new JLabel(prompt), c);

			c.gridy = 1;
			c.gridwidth = 1;

			add(new JLabel("username: "), c);
			c.gridx = 2;
			add(userNameField, c);
			c.gridx = 1;
			c.gridy++;

			if (credentials.getUserName() != null) {
				userNameField.setText(credentials.getUserName());
			}
			
			if (credentials.getPasswd() == null) {
				add(new JLabel("passwd:  "), c);
				c.gridx = 2;
				add(passwordField, c);
				c.gridx = 1;
				c.gridy++;
			} else {
				passwordField.setText(credentials.getPasswd());
			}
			
			if (passfile != null) {
				c.gridwidth = 2;
				add(rememberDataCB, c);
				c.gridy++;
			}
			c.gridwidth = 2;
			add(new JLabel(), c); // spacer

		}
	}


	public static Credentials promptCredentials(Credentials c, File passfile) {
		c = loadPassfile(c, passfile);
		if (c.getUserName() != null && c.getPasswd() != null) {
			return c;
		}
		CredentialPanel credentialPanel = new CredentialPanel(c, passfile);
		if (JOptionPane.showOptionDialog(null, credentialPanel, c.getHost()+" credentials", JOptionPane.OK_CANCEL_OPTION, 0, new ImageIcon(Ivy.class.getResource("logo.png")), null, new Integer(JOptionPane.OK_OPTION)) == JOptionPane.OK_OPTION) {
			String username=credentialPanel.userNameField.getText();
			String passwd=credentialPanel.passwordField.getText();
			if (credentialPanel.rememberDataCB.isSelected()) {
				Properties props = new EncrytedProperties();
				props.setProperty("username", username);
				props.setProperty("passwd", passwd);
				FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(passfile);
					props.store(fos, "");
				} catch (Exception e) {
					Message.warn("error occured while saving password file "+passfile+": "+ e);
				} finally {
					if (fos != null) {try {fos.close();} catch (Exception e) {}}
				}
			}
			c = new Credentials(c.getRealm(), c.getHost(), username, passwd);
		}
		return c;
	}


	public static Credentials loadPassfile(Credentials c, File passfile) {
		if (passfile != null && passfile.exists()) {
			Properties props = new EncrytedProperties();
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(passfile);
				props.load(fis);
				String username = c.getUserName();
				String passwd = c.getPasswd();
				if (username == null) {
					username = props.getProperty("username");
				}
				if (passwd == null) {
					passwd = props.getProperty("passwd");
				}
				return new Credentials(c.getRealm(), c.getHost(), username, passwd);
			} catch (IOException e) {
				Message.warn("error occured while loading password file "+passfile+": "+ e);
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException e) {
					}
				}
			}
		}
		return c;
	}


}
