package grith.jgrith.view.swing.proxyInit;

import grisu.jcommons.commonInterfaces.ProxyCreatorHolder;
import grisu.jcommons.commonInterfaces.ProxyCreatorPanel;
import grisu.jcommons.commonInterfaces.ProxyDestructorHolder;
import grith.jgrith.view.swing.ProxyInitListener;
import grith.jgrith.view.swing.VomsProxyInfoPanel;

import java.beans.Beans;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;

import org.globus.gsi.GlobusCredential;
import org.globus.myproxy.MyProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class GenericProxyCreationPanel extends JPanel implements
ProxyCreatorHolder, ProxyDestructorHolder {

	private OtherActionsPanel otherActionsPanel;
	private JPanel panel;
	private CreateVomsProxyPanel createVomsProxyPanel;
	private VomsProxyInfoPanel vomsProxyInfoPanel;
	private static final Logger myLogger = LoggerFactory
			.getLogger(GenericProxyCreationPanel.class);

	private MyProxyProxyCreatorPanel myProxyProxyCreatorPanel;
	private LocalX509CertProxyCreatorPanel localX509CertProxyCreatorPanel;
	private ProxyCreatorPanel shibProxyCreatorPanel;

	private static final String MYPROXY_TAB_NAME = "MyProxy";
	private static final String LOCAL_CERT_TAB_NAME = "Certificate login";
	private static final String SHIB_TAB_NAME = "Institution login";

	private String shibUrl = null;

	private GlobusCredential currentProxy = null;

	private JTabbedPane tabbedPane;
	private JButton button;

	private final boolean useShib, useX509, useMyProxy;
	private boolean displayOtherAction = true;

	// -------------------------------------------------------------------
	// EventStuff
	private Vector<ProxyInitListener> proxyListeners;

	/**
	 * Create the default version of the panel
	 */
	public GenericProxyCreationPanel() {
		this(true, true, true, true, null);
	}

	public GenericProxyCreationPanel(boolean useShib, boolean useX509,
			boolean useMyProxy, boolean displayOtherAction) {
		this(useShib, useX509, useMyProxy, displayOtherAction, null);
	}

	public GenericProxyCreationPanel(boolean useShib, boolean useX509,
			boolean useMyProxy, boolean displayOtherAction, String shibUrl) {

		super();
		this.useShib = useShib;
		this.useX509 = useX509;
		this.useMyProxy = useMyProxy;
		this.displayOtherAction = displayOtherAction;
		this.shibUrl = shibUrl;
		initialize();
	}

	// register a listener
	synchronized public void addProxyListener(ProxyInitListener l) {
		if (proxyListeners == null) {
			proxyListeners = new Vector();
		}
		proxyListeners.addElement(l);
	}

	public void destroyProxy() {

		this.proxyCreated(null);
	}

	private void fireNewProxyCreated(GlobusCredential proxy) {

		// if we have no mountPointsListeners, do nothing...
		if ((proxyListeners != null) && !proxyListeners.isEmpty()) {
			// create the event object to send

			// make a copy of the listener list in case
			// anyone adds/removes mountPointsListeners
			Vector targets;
			synchronized (this) {
				targets = (Vector) proxyListeners.clone();
			}

			// walk through the listener list and
			// call the gridproxychanged method in each
			Enumeration e = targets.elements();
			while (e.hasMoreElements()) {
				ProxyInitListener l = (ProxyInitListener) e.nextElement();
				l.proxyCreated(proxy);
			}
		}
	}

	/**
	 * @return
	 */
	protected CreateVomsProxyPanel getCreateVomsProxyPanel() {
		if (createVomsProxyPanel == null) {
			createVomsProxyPanel = new CreateVomsProxyPanel();
			createVomsProxyPanel.setBorder(new TitledBorder(null,
					"Add group to proxy", TitledBorder.DEFAULT_JUSTIFICATION,
					TitledBorder.DEFAULT_POSITION, null, null));
			createVomsProxyPanel.setProxyCreatorHolder(this);
		}
		return createVomsProxyPanel;
	}

	/**
	 * @return
	 */
	protected LocalX509CertProxyCreatorPanel getLocalX509CertProxyCreatorPanel() {
		if (localX509CertProxyCreatorPanel == null) {
			localX509CertProxyCreatorPanel = new LocalX509CertProxyCreatorPanel();
		}
		return localX509CertProxyCreatorPanel;
	}

	public MyProxy getMyProxy() {
		return getMyProxyProxyCreatorPanel().getMyproxy();
	}

	/**
	 * @return
	 */
	protected MyProxyProxyCreatorPanel getMyProxyProxyCreatorPanel() {
		if (myProxyProxyCreatorPanel == null) {
			myProxyProxyCreatorPanel = new MyProxyProxyCreatorPanel();
		}
		return myProxyProxyCreatorPanel;
	}

	/**
	 * @return
	 */
	protected OtherActionsPanel getOtherActionsPanel() {
		if (otherActionsPanel == null) {
			otherActionsPanel = new OtherActionsPanel(true);
			otherActionsPanel.setBorder(new TitledBorder(null, "Other actions",
					TitledBorder.DEFAULT_JUSTIFICATION,
					TitledBorder.DEFAULT_POSITION, null, null));
		}
		return otherActionsPanel;
	}

	/**
	 * @return
	 */
	protected JPanel getPanel() {
		if (panel == null) {
			panel = new JPanel();
			panel.setBorder(new TitledBorder(null, "Create/retrieve proxy",
					TitledBorder.DEFAULT_JUSTIFICATION,
					TitledBorder.DEFAULT_POSITION, null, null));
			panel.setLayout(new FormLayout(new ColumnSpec[] { ColumnSpec
					.decode("334px:grow"), }, new RowSpec[] {
					RowSpec.decode("154px:grow"),
					FormFactory.RELATED_GAP_ROWSPEC, }));
			panel.add(getTabbedPane(), new CellConstraints(
					"1, 1, 1, 1, fill, fill"));
		}
		return panel;
	}

	/**
	 * Returns the currently selected idp
	 * 
	 * @return the idp or null if the panel isn't used...
	 */
	public String getSelectedIdp() {

		if (useShib) {

			return getShibbolethProxyCreatorPanel().getCurrentSettings().get(
					ProxyCreatorPanel.CURRENT_IDP_KEY);

		} else {
			return null;
		}

	}

	protected ProxyCreatorPanel getShibbolethProxyCreatorPanel() {
		if (shibProxyCreatorPanel == null) {

			try {
				// Class shibPanelClass = Class
				// .forName("au.org.mams.slcs.client.view.swing.SlcsLoginPanel");
				// shibProxyCreatorPanel = (ProxyCreatorPanel) shibPanelClass
				// .newInstance();

				shibProxyCreatorPanel = new SlcsPanel(shibUrl);
				shibProxyCreatorPanel.setProxyCreatorHolder(this);

			} catch (Exception e) {
				myLogger.error("Can't create shibProxyPanel: "
						+ e.getLocalizedMessage(), e);
				throw new RuntimeException(
						"Can't create shibboleth authentication panel.", e);
			}

		}
		return shibProxyCreatorPanel;
	}

	/**
	 * @return
	 */
	protected JTabbedPane getTabbedPane() {
		if (tabbedPane == null) {
			tabbedPane = new JTabbedPane();
			if (useShib) {
				if (getShibbolethProxyCreatorPanel() != null) {
					tabbedPane.addTab(SHIB_TAB_NAME,
							getShibbolethProxyCreatorPanel().getPanel());
					getShibbolethProxyCreatorPanel()
					.setProxyCreatorHolder(this);
				}
			}
			if (useX509) {
				tabbedPane.addTab(LOCAL_CERT_TAB_NAME, null,
						getLocalX509CertProxyCreatorPanel(), null);
			}
			if (useMyProxy) {
				tabbedPane.addTab(MYPROXY_TAB_NAME, null,
						getMyProxyProxyCreatorPanel(), null);
			}

			if (!Beans.isDesignTime()) {
				if (useX509) {
					getLocalX509CertProxyCreatorPanel().setProxyCreatorHolder(
							this);
				}
				if (useMyProxy) {
					getMyProxyProxyCreatorPanel().setProxyCreatorHolder(this);
				}
				if (displayOtherAction) {
					getOtherActionsPanel().setProxyDescrutorHolder(this);
					getOtherActionsPanel().setProxyCreationHolder(this);
				}
			}

		}
		return tabbedPane;
	}

	/**
	 * @return
	 */
	protected VomsProxyInfoPanel getVomsProxyInfoPanel() {
		if (vomsProxyInfoPanel == null) {
			vomsProxyInfoPanel = new VomsProxyInfoPanel();
		}
		return vomsProxyInfoPanel;
	}

	private void initialize() {
		setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("23dlu:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("35dlu:grow"),
				FormFactory.RELATED_GAP_COLSPEC, }, new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC, RowSpec.decode("167dlu"),
				FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC, }));
		add(getVomsProxyInfoPanel(), new CellConstraints(2, 6, 3, 1,
				CellConstraints.FILL, CellConstraints.DEFAULT));
		add(getCreateVomsProxyPanel(), new CellConstraints(2, 4, 3, 1));
		add(getPanel(), new CellConstraints(2, 2, 3, 1, CellConstraints.FILL,
				CellConstraints.FILL));
		if (displayOtherAction) {
			add(getOtherActionsPanel(), new CellConstraints(2, 8, 3, 1,
					CellConstraints.FILL, CellConstraints.FILL));
		}
		//
	}

	public void proxyCreated(GlobusCredential proxy) {

		this.currentProxy = proxy;

		if (proxy != null) {
			myLogger.debug("Proxy created:");
			myLogger.debug("Subject:\t" + proxy.getSubject());
			myLogger.debug("Issuer:\t" + proxy.getIssuer());
			myLogger.debug("Time left:\t" + proxy.getTimeLeft());
		}

		getVomsProxyInfoPanel().proxyCreated(proxy);
		getCreateVomsProxyPanel().setProxy(proxy);
		if (displayOtherAction) {
			getOtherActionsPanel().setProxy(proxy);
		}

		fireNewProxyCreated(proxy);

	}

	public void proxyCreationFailed(String message) {

		JOptionPane.showMessageDialog(GenericProxyCreationPanel.this, message,
				"Voms error", JOptionPane.ERROR_MESSAGE);

	}

	// remove a listener
	synchronized public void removeProxyListener(ProxyInitListener l) {
		if (proxyListeners == null) {
			proxyListeners = new Vector<ProxyInitListener>();
		}
		proxyListeners.removeElement(l);
	}

	public void setMyProxy(MyProxy myproxy) {
		getMyProxyProxyCreatorPanel().setMyproxy(myproxy);
	}
}
