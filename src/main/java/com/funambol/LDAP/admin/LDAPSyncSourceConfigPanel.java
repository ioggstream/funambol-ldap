/**
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.funambol.LDAP.admin;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import org.apache.commons.lang.StringUtils;

import com.funambol.LDAP.engine.source.LDAPContactsSyncSource;
import com.funambol.LDAP.utils.Constants;
import com.funambol.admin.AdminException;
import com.funambol.admin.ui.SourceManagementPanel;
import com.funambol.framework.engine.source.ContentType;
import com.funambol.framework.engine.source.SyncSourceInfo;

/**
 * This class implements the configuration panel for an LDAPSyncSource
 *
 * @author  <a href='mailto:fipsfuchs _@ users.sf.net'>Philipp Kamps</a>
 * @author  <a href='mailto:julien.buratto _@ subitosms.it'>Julien Buratto</a>
 * @author  <a href='mailto:gdartigu _@ smartjog.com'>Gilles Dartiguelongue</a>
 * @author  <a href='mailto:robipolli _@ gmail.com'>Roberto Polli</a>
 * @author  <a href='mailto:dyerger _@ stcservices.com'>David Yerger</a>
 * @version $Id$
 *
 * Change log:
 *  -David Yerger: add support for Active Directory
 * - Roberto Polli: add support for remote addressbook
 * - Julien Buratto: added tooltips to labels
 * - Julien Buratto: removed claim support for SIF - deprecated
 *
 */
public class LDAPSyncSourceConfigPanel extends SourceManagementPanel implements Serializable, Constants
{
	private static final long serialVersionUID = 4964775829968189719L;

	// --------------------------------------------------------------- Constants

	/**
	 * Allowed characters for name and uri
	 */
	public static final String NAME_ALLOWED_CHARS
	= "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-_.";

	private static final String[]         supportedTypesS    = SUPPORTED_TYPES;
	private static final String[]         supportedTypesV    = SUPPORTED_TYPES_VERSION;


	/** label for the panel's name */
	private JLabel panelName = new JLabel();

	/** border to evidence the title of the panel */
	private TitledBorder  titledBorder1;

	private JLabel           nameLabel          = new JLabel();
	private JTextField       nameValue          = new JTextField();
	private JLabel           typeLabel          = new JLabel();
	private ContentType[]    supportedTypes     = null;

	private JComboBox        typeValue          = new JComboBox(supportedTypesS);
	private JLabel           timeZoneLabel      = new JLabel("Server timezone :");
	private JComboBox        timeZoneValue      = null;
	private JLabel           sourceUriLabel     = new JLabel();
	private JTextField       sourceUriValue     = new JTextField();

	private JComboBox        ldapServerValue          = new JComboBox(SUPPORTED_SERVERS);
	private JLabel           ldapServerLabel      = new JLabel();

	private JLabel           providerUrlLabel      = new JLabel()    ;
	private JTextField       providerUrlValue      = new JTextField();

	private JLabel           ldapBaseLabel      = new JLabel()    ;
	private JTextField       ldapBaseValue      = new JTextField();

	private JLabel		ldapUserLabel      = new JLabel()    ;
	private JTextField	ldapUserValue      = new JTextField();
	private JLabel           ldapPassLabel      = new JLabel()    ;
	private JPasswordField   ldapPassValue      = new JPasswordField();

	// do I have to bind with the provisioned user?
	private JLabel           followReferralLabel         = new JLabel();
	private JCheckBox        followReferralValue         = new JCheckBox();
	
	private JLabel           connectionPoolingLabel         = new JLabel();
	private JCheckBox        connectionPoolingValue         = new JCheckBox();
	
	private JLabel           dbNameLabel        = new JLabel()    ;
	private JTextField       dbNameValue        = new JTextField();

	private JButton          confirmButton      = new JButton()    ;

	private JLabel daoNameLabel  = new JLabel() ;
	private JComboBox daoNameValue= new JComboBox(SUPPORTED_DAO);

	private JLabel entryFilterLabel = new JLabel();
	private JTextField entryFilterValue= new JTextField();

	private static java.awt.Font FONT_ARIAL = new java.awt.Font("Arial", 0, 12);
	// ------------------------------------------------------------ Constructors

	/**
	 * Creates a new LDAPSyncSourceConfigPanel instance
	 */
	public LDAPSyncSourceConfigPanel()
	{
		init();

		List<ContentType> ct = new ArrayList<ContentType>();

		for(int i=0; i<supportedTypesS.length; i++) {
			ct.add( new ContentType(supportedTypesS[i], supportedTypesV[i]) );
		}

		supportedTypes = new ContentType[supportedTypesS.length];
		ct.toArray(supportedTypes);


	}

	// ----------------------------------------------------------- Private methods

	/**
	 * Create the panel
	 * @throws Exception if error occures during creation of the panel
	 */
	private void init()
	{
		// set layout
		this.setLayout(null);

		int startX = 14;
		int fontHeight = 18;
		int col1Size = 150;
		int col2X = startX + col1Size + 6; //170px 
		int col2Size= 350; 
		int col3X = col2X+col2Size + 6; // 550
		int col3Size = 90;
		int chkboxSize = 18;
		// set properties of label, position and border
		// referred to the title of the panel
		titledBorder1 = new TitledBorder("");

		panelName.setFont(titlePanelFont);
		panelName.setText("Edit LDAP SyncSourceContacts");
		panelName.setBounds(new Rectangle(startX, 5, 316, 28));
		panelName.setAlignmentX(SwingConstants.CENTER);
		panelName.setBorder(titledBorder1);

		int baseline = 60;
		sourceUriLabel.setText("Source URI: ");
		sourceUriLabel.setToolTipText("Choose a unique word");
		sourceUriLabel.setFont(defaultFont);
		sourceUriLabel.setBounds(new Rectangle(startX,baseline, col1Size, fontHeight));
		sourceUriValue.setFont(FONT_ARIAL);
		sourceUriValue.setBounds(new Rectangle(col2X, baseline, col2Size, fontHeight));

		baseline += 30;		
		nameLabel.setText("Name: ");
		nameLabel.setToolTipText("Choose a word and set this into your client in order to use this connector");
		nameLabel.setFont(defaultFont);
		nameLabel.setBounds(new Rectangle(startX, baseline, col1Size, fontHeight));
		nameValue.setFont(FONT_ARIAL);
		nameValue.setBounds(new Rectangle(col2X, baseline, col2Size, fontHeight));

		baseline += 30;		
		typeLabel.setText("Type: ");
		typeLabel.setToolTipText("Only VCARD are supported, SIF is deprecated");
		typeLabel.setFont(defaultFont);
		typeLabel.setBounds(new Rectangle(startX, baseline, col1Size, fontHeight));
		typeValue.setBounds(new Rectangle(col2X, baseline, col2Size, fontHeight));

		baseline += 30;		
		providerUrlLabel.setText("LDAP URI: ");
		providerUrlLabel.setToolTipText("eg. ldap://ldap.example.com , ldaps://ldap.example.com:390 ");
		providerUrlLabel.setFont(defaultFont);
		providerUrlLabel.setBounds(new Rectangle(startX, baseline, col1Size, fontHeight));
		providerUrlValue.setFont(FONT_ARIAL);
		providerUrlValue.setBounds(new Rectangle(col2X, baseline, col2Size, fontHeight));

		baseline += 30;		
		ldapBaseLabel.setText("LDAP Base DN: ");
		ldapBaseLabel.setToolTipText("This is used to define where to store/read user's data.\nRead install.txt notes to use parameters");
		ldapBaseLabel.setFont(defaultFont);
		ldapBaseLabel.setBounds(new Rectangle(startX, baseline, col1Size, fontHeight));
		ldapBaseValue.setFont(FONT_ARIAL);
		ldapBaseValue.setBounds(new Rectangle(col2X, baseline, col2Size, fontHeight));

		baseline += 30;		// TODO contactDaoLabel, entryFilterLabel
		daoNameLabel.setText("DAO Class for converting item to LDAP");
		daoNameLabel.setToolTipText("piTypePerson, inetOrgPerson or organizationalPerson");
		daoNameLabel.setFont(defaultFont);
		daoNameLabel.setBounds(new Rectangle(startX, baseline, col1Size, fontHeight));
		daoNameValue.setFont(FONT_ARIAL);
		daoNameValue.setBounds(new Rectangle(col2X, baseline, col2Size, fontHeight));

		baseline += 30;		
		entryFilterLabel.setText("Filter user by");
		entryFilterLabel.setToolTipText("A valid  LDAP search filter, eg: (&(objectclass=inetOrgPerson)(active=1))");
		entryFilterLabel.setFont(defaultFont);
		entryFilterLabel.setBounds(new Rectangle(startX, baseline, col1Size, fontHeight));
		entryFilterValue.setFont(FONT_ARIAL);
		entryFilterValue.setBounds(new Rectangle(col2X, baseline, col2Size, fontHeight));

		baseline += 30;		
		ldapUserLabel.setText("LDAP User: ");
		ldapUserLabel.setToolTipText("LDAP Bind DN (username) to access the LDAP server");
		ldapUserLabel.setFont(defaultFont);
		ldapUserLabel.setBounds(new Rectangle(startX, baseline, col1Size, fontHeight));
		ldapUserValue.setFont(FONT_ARIAL);
		ldapUserValue.setBounds(new Rectangle(col2X, baseline, col2Size, fontHeight));

		baseline += 30;
		ldapPassLabel.setText("LDAP Password: ");
		ldapPassLabel.setToolTipText("LDAP Bind DN password to access LDAP server");
		ldapPassLabel.setFont(defaultFont);
		ldapPassLabel.setBounds(new Rectangle(startX, baseline, col1Size, fontHeight));
		ldapPassValue.setFont(FONT_ARIAL);
		ldapPassValue.setBounds(new Rectangle(col2X, baseline, col2Size, fontHeight));

		// follow referral
		followReferralLabel.setText("Follow Referral ");
		followReferralLabel.setToolTipText("Select this checkbox if you want ldap to follow smart-referrals");
		followReferralLabel.setFont(defaultFont);
		followReferralLabel.setBounds(new Rectangle(col3X, 270, col3Size, fontHeight));
		followReferralValue.setSelected(false);
		followReferralValue.setBounds(new Rectangle(col3X, 300, col3Size, fontHeight));
		// connection pooling
		connectionPoolingLabel.setText("Pooling ");
		connectionPoolingLabel.setToolTipText("Select this checkbox if you want use connection pooling");
		connectionPoolingLabel.setFont(defaultFont);
		connectionPoolingLabel.setBounds(new Rectangle(col3X+col3Size, 270, col3Size, fontHeight));
		connectionPoolingValue.setSelected(false);
		connectionPoolingValue.setBounds(new Rectangle(col3X+col3Size, 300, col3Size, fontHeight));
		
		baseline += 30;
		dbNameLabel.setText("Funambol DBMS Name: ");
		dbNameLabel.setToolTipText("Funambol DS table to store metadata. Use fnblcore");
		dbNameLabel.setFont(defaultFont);
		dbNameLabel.setBounds(new Rectangle(startX, baseline, col1Size, fontHeight));
		dbNameValue.setFont(FONT_ARIAL);
		dbNameValue.setBounds(new Rectangle(col2X, baseline, col2Size, fontHeight));

		baseline += 30;
		timeZoneLabel.setFont(defaultFont);
		timeZoneLabel.setBounds(new Rectangle(startX, baseline, col1Size, fontHeight));
		timeZoneValue = new JComboBox(TimeZone.getAvailableIDs());
		timeZoneValue.setBounds(new Rectangle(col2X, baseline, col2Size, fontHeight));
		// rpolli
		baseline += 30;
		ldapServerLabel.setText("LDAP Server Type");
		ldapServerLabel.setFont(defaultFont);
		ldapServerLabel.setBounds(new Rectangle(startX, baseline, col1Size, fontHeight));
		ldapServerValue.setBounds(new Rectangle(col2X, baseline, col2Size, fontHeight));
		ldapServerValue.setToolTipText("Select compatibility unique ID's per items.");
		confirmButton.setFont(defaultFont);
		confirmButton.setText("Add");
		confirmButton.setBounds(col2X, 420, 70, 25);

		confirmButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event )
			{
				try {
					validateValues();
					getValues();
					if (getState() == STATE_INSERT) {
						LDAPSyncSourceConfigPanel.this.actionPerformed(new ActionEvent(LDAPSyncSourceConfigPanel.this, ACTION_EVENT_INSERT, event.getActionCommand()));
					} else {
						LDAPSyncSourceConfigPanel.this.actionPerformed(new ActionEvent(LDAPSyncSourceConfigPanel.this, ACTION_EVENT_UPDATE, event.getActionCommand()));
					}
				} catch (Exception e)				{
					e.printStackTrace();
					notifyError(new AdminException(e.getMessage()));
				}
			}
		});

		// add all components to the panel
		this.add(panelName        , null);
		this.add(nameLabel        , null);
		this.add(nameValue        , null);
		this.add(typeLabel        , null);
		this.add(typeValue        , null);
		this.add(sourceUriLabel   , null);
		this.add(sourceUriValue   , null);

		//rpolli
		this.add(ldapServerLabel, null);
		this.add(ldapServerValue, null);
		this.add(this.daoNameLabel,null);
		this.add(this.daoNameValue,null);

		this.add(followReferralLabel,null);
		this.add(followReferralValue,null);
		this.add(connectionPoolingLabel,null);
		this.add(connectionPoolingValue,null);
		
		this.add(providerUrlLabel    , null);
		this.add(providerUrlValue    , null);
		//		this.add(ldapPortLabel    , null);
		//		this.add(ldapPortValue    , null);
		//		this.add(isSSLLabel       , null);
		//		this.add(isSSLValue       , null);
		this.add(ldapBaseLabel    , null);
		this.add(ldapBaseValue    , null);
		this.add(entryFilterLabel    , null);
		this.add(entryFilterValue   , null);

		this.add(ldapUserLabel    , null);
		this.add(ldapUserValue    , null);
		this.add(ldapPassLabel    , null);
		this.add(ldapPassValue    , null);
		this.add(timeZoneLabel    , null);
		this.add(timeZoneValue    , null);

		this.add(dbNameLabel      , null);
		this.add(dbNameValue      , null);

		this.add(confirmButton    , null);
	}

	/**
	 * Load the current syncSource showing the name, uri and type in the panel's
	 * fields.
	 */
	public void updateForm()
	{
		if (!(getSyncSource() instanceof LDAPContactsSyncSource))
		{
			notifyError(
					new AdminException(
							"This is not an "+LDAPContactsSyncSource.class.getName()+" Unable to process SyncSource values ("+getSyncSource()+")."
					)
			);
			return ;
		}

		LDAPContactsSyncSource syncSource = (LDAPContactsSyncSource) getSyncSource();

		if (getState() == STATE_INSERT) {
			confirmButton.setText("Add");
		} else if (getState() == STATE_UPDATE) {
			confirmButton.setText("Save");
		}

		syncSource.setInfo( new SyncSourceInfo(supportedTypes, typeValue.getSelectedIndex()) );
		sourceUriValue.setText(syncSource.getSourceURI() );
		nameValue.setText     (syncSource.getName()      );
		typeValue.setSelectedIndex( syncSource.getInfo().getPreferred() );

		if (syncSource.getServerTimeZone() != null) {
			timeZoneValue.setSelectedItem(syncSource.getServerTimeZone().getID());
		} else {
			timeZoneValue.setSelectedItem(TimeZone.getDefault().getID());
		}

		daoNameValue.setSelectedItem(syncSource.getDaoName());
		followReferralValue.setSelected(syncSource.isFollowReferral());
		connectionPoolingValue.setSelected(syncSource.isConnectionPooling());
		providerUrlValue.setText (syncSource.getProviderUrl()   );


		//	bindAsUserValue.setSelected(syncSource.getBindAsUser());

		ldapBaseValue.setText (syncSource.getLdapBase());
		entryFilterValue.setText(syncSource.getEntryFilter());
		ldapUserValue.setText (syncSource.getLdapUser());
		ldapPassValue.setText (syncSource.getLdapPass());

		dbNameValue.setText(StringUtils.defaultIfEmpty(syncSource.getDbName(), "fnblds"));

		if (syncSource.getSourceURI() != null) {
			sourceUriValue.setEditable(false);
		}
		
		ldapServerValue.setSelectedItem(syncSource.getLdapInterfaceClassName());
	}

	// ----------------------------------------------------------- Private methods
	/**
	 * Checks if the values provided by the user are all valid. In caso of errors,
	 * an IllegalArgumentException is thrown.
	 *
	 * @throws IllegalArgumentException if:
	 *         <ul>
	 *         <li>name, uri, type or directory are empty (null or zero-length)
	 *         <li>the types list length does not match the versions list length
	 *         </ul>
	 */
	private void validateValues() throws IllegalArgumentException
	{
		String value = nameValue.getText();

		if (StringUtils.isEmpty(value)) {
			throw new IllegalArgumentException("Field 'Name' cannot be empty. Please provide a SyncSource name.");
		}

		if (!StringUtils.containsOnly(value, NAME_ALLOWED_CHARS.toCharArray())) {
			throw new IllegalArgumentException("Only the following characters are allowed for field 'Name': \n" + NAME_ALLOWED_CHARS);
		}

		value = sourceUriValue.getText();
		if (StringUtils.isEmpty(value)) {
			throw new IllegalArgumentException("Field 'Source URI' cannot be empty. Please provide a SyncSource URI.");
		}

		value = dbNameValue.getText();
		if (StringUtils.isEmpty(value)) {
			throw new	IllegalArgumentException("Field 'Sync DBMS name' cannot be empty. Please provide a name.");
		}

		value = (String) ldapServerValue.getSelectedItem();
		if (StringUtils.isEmpty(value)) {
			throw new	IllegalArgumentException("Field 'LDAP Server Type' cannot be empty. Please provide a name.");
		}

		value = (String) daoNameValue.getSelectedItem();
		if (StringUtils.isEmpty(value)) {
			throw new	IllegalArgumentException("Field 'DAO objectclass' cannot be empty. Please provide a name.");
		}
	}

	/**
	 * Set syncSource properties with the values provided by the user.
	 */
	private void getValues()
	{
		LDAPContactsSyncSource syncSource = (LDAPContactsSyncSource)getSyncSource();

		syncSource.setSourceURI(sourceUriValue.getText().trim());
		syncSource.setName(nameValue.getText().trim());
		syncSource.setProviderUrl(providerUrlValue.getText().trim());
		syncSource.setFollowReferral(followReferralValue.isSelected());
		syncSource.setConnectionPooling(connectionPoolingValue.isSelected());

		syncSource.setLdapBase       (ldapBaseValue.getText().trim());
		syncSource.setLdapUser       (ldapUserValue.getText().trim());
		syncSource.setLdapPass       (new String(ldapPassValue.getPassword()));
		syncSource.setServerTimeZone (TimeZone.getTimeZone( (String)timeZoneValue.getSelectedItem()));
		syncSource.setDbName         (dbNameValue.getText().trim());

		syncSource.setEntryFilter(entryFilterValue.getText().trim());


		if (DAO_INETORGPERSON.equals(daoNameValue.getSelectedItem())) {
			syncSource.setDaoName(DAO_INETORGPERSON);
		} else if (DAO_PITYPEPERSON.equals(daoNameValue.getSelectedItem())) {
			syncSource.setDaoName(DAO_PITYPEPERSON);
		} else if (DAO_ORGANIZATIONALPERSON.equals(daoNameValue.getSelectedItem())) {
			syncSource.setDaoName(DAO_ORGANIZATIONALPERSON);
		} else {
			notifyError(new AdminException("Bad ContactDAO class: " + daoNameValue.getSelectedItem()));
		}

		if (SERVER_OPENLDAP.equals(ldapServerValue.getSelectedItem())) {
			syncSource.setLdapInterfaceClassName(SERVER_OPENLDAP);
		} else if  (SERVER_FEDORA.equals(ldapServerValue.getSelectedItem())) {
			syncSource.setLdapInterfaceClassName(SERVER_FEDORA);
		} else if  (SERVER_ACTIVEDIRECTORY.equals(ldapServerValue.getSelectedItem())) {
			syncSource.setLdapInterfaceClassName(SERVER_ACTIVEDIRECTORY);
		} else {
			notifyError(new AdminException("Bad Manager class: " + ldapServerValue.getSelectedItem()));
		}

		syncSource.setInfo( new SyncSourceInfo(supportedTypes, typeValue.getSelectedIndex()) );
	}
}
