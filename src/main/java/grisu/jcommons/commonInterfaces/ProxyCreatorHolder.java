package grisu.jcommons.commonInterfaces;

import org.globus.gsi.GlobusCredential;

public interface ProxyCreatorHolder {

	public void proxyCreated(GlobusCredential proxy);

	public void proxyCreationFailed(String message);

}
