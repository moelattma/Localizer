package edu.ucsf.rbvi.localizer.internal;

import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.TITLE;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_LONG_DESCRIPTION;
import java.util.Properties;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.BundleContext;

import edu.ucsf.rbvi.localizer.internal.tasks.LocalizeTaskFactory;
import edu.ucsf.rbvi.localizer.internal.tasks.RemoveCloneNodeTaskFactory;
import edu.ucsf.rbvi.localizer.internal.model.LocalizerManager;

public class CyActivator extends AbstractCyActivator {

	public CyActivator() {
		super();
	}

	@Override
	public void start(BundleContext bc) {
		// See if we have a graphics console or not
		boolean haveGUI = true;
		final CySwingApplication cyApplication = getService(bc, CySwingApplication.class);
		final CyServiceRegistrar registrar = getService(bc, CyServiceRegistrar.class);

		if (cyApplication == null) {
			haveGUI = false;
			// Issue error
		}

		LocalizerManager manager = new LocalizerManager(registrar);

		{
			LocalizeTaskFactory localizeFactory = new LocalizeTaskFactory(manager);
			Properties localizeProps = new Properties();
			localizeProps.setProperty(PREFERRED_MENU, "Apps.localizer");
			localizeProps.setProperty(TITLE, localizeFactory.getTitle());
			localizeProps.setProperty(MENU_GRAVITY, "1.0");
			localizeProps.setProperty(IN_MENU_BAR, "true");
			registerService(bc, localizeFactory, NetworkTaskFactory.class, localizeProps);
		}

		RemoveCloneNodeTaskFactory removeClonedNodes = new RemoveCloneNodeTaskFactory(manager);
		Properties removeCloneProps = new Properties();
		removeCloneProps.setProperty(PREFERRED_MENU, "Apps.localizer");
		removeCloneProps.setProperty(TITLE, removeClonedNodes.getTitle());
		removeCloneProps.setProperty(MENU_GRAVITY, "2.0");
		removeCloneProps.setProperty(IN_MENU_BAR, "true");
		registerService(bc, removeClonedNodes, NetworkTaskFactory.class, removeCloneProps);

		LocalizeTaskFactory localizeFactory = new LocalizeTaskFactory(manager);
		Properties localizeProps = new Properties();
		localizeProps.setProperty(COMMAND_NAMESPACE, "localizer");
		localizeProps.setProperty(COMMAND, "localize network");
		localizeProps.setProperty(COMMAND_DESCRIPTION, "Localize a STRING network");
		localizeProps.setProperty(COMMAND_LONG_DESCRIPTION, "Localize an already existing STRING network"
				+ " based on some localization such as compartments or tissues");
		registerService(bc, localizeFactory, TaskFactory.class, localizeProps);
	}
}
