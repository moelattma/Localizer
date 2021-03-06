package edu.ucsf.rbvi.localizer.internal.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cytoscape.command.StringToModel;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.localizer.internal.model.LocalizeNode;
import edu.ucsf.rbvi.localizer.internal.model.LocalizerManager;

public class LocalizeTask extends AbstractTask {
	private static final String ID_COMPARTMENTS = "compartment";
	private static final String ID_TISSUES = "tissue";

	private final LocalizerManager manager;

	@Tunable(description = "Network to localize", 
			longDescription = StringToModel.CY_NETWORK_LONG_DESCRIPTION, 
			exampleStringValue = StringToModel.CY_NETWORK_EXAMPLE_STRING, 
			context = "nogui", required=true)
	public CyNetwork network;

	@Tunable(description = "Select what to localize", 
			longDescription = "Select which STRING localization on which to localize this network", 
			exampleStringValue = ID_COMPARTMENTS, required=true)
	public ListSingleSelection<String> toLocalize;

	@Tunable(description = "Optimize localization using guilt by association")
	public boolean useGBA = true;

	@Tunable(description = "Clone nodes with uncertain localization")
	public boolean cloneNodes = true;

	public LocalizeTask(LocalizerManager manager, CyNetwork network) {
		this.manager = manager;
		if (network == null)
			network = manager.getCurrentNetwork();

		this.network = network;
		List<String> localizeOptions = new ArrayList<>();
		localizeOptions.add(ID_COMPARTMENTS);
		localizeOptions.add(ID_TISSUES);
		toLocalize = new ListSingleSelection<>(localizeOptions);
	}

	@Override
	public void run(TaskMonitor monitor) throws Exception {
		if (network == null) {
			monitor.showMessage(TaskMonitor.Level.WARN, "No network to localize");
		} else if(toLocalize.getSelectedValue() == null || 
				!toLocalize.getPossibleValues().contains(toLocalize.getSelectedValue())) { 
			monitor.showMessage(TaskMonitor.Level.WARN, "No proper localization has been given");
		} else if(!LocalizerManager.isStringNetwork(network)) {
			monitor.showMessage(TaskMonitor.Level.WARN, "This network is not a string network");
		}

		String localizer = toLocalize.getSelectedValue();
		CyTable nodeTable = network.getDefaultNodeTable();
		manager.removeClonedNodes(network);
		if(cloneNodes) 
			nodeTable.createColumn(LocalizerManager.CLONE_ID, Boolean.class, false);
		if (nodeTable.getColumn(localizer) != null)
			nodeTable.deleteColumn(localizer);
		nodeTable.createColumn(localizer, String.class, false);

		Map<CyNode, LocalizeNode> localizeNodes = manager.getLocalizeNodes(network, localizer);
		Map<CyNode, List<String>> setLocalizations = new HashMap<>();

		Random rand = new Random();
		for (LocalizeNode localizeNode : localizeNodes.values()) {
			CyNode node = network.getNode(localizeNode.getNodeSUID());
			List<String> locals = localizeNode.getLocalizations();
			if (useGBA && locals.size() > 1) {
				List<CyNode> nodes = network.getNeighborList(node, CyEdge.Type.ANY);
				Map<String, Integer> gbaMap = new HashMap<>();
				for (String local : locals)
					gbaMap.put(local, 0);
				for (CyNode compared : nodes) {
					LocalizeNode localCompare = localizeNodes.get(compared);
					for (String local : locals)
						gbaMap.put(local, gbaMap.get(local) + localCompare.getConfidence(local));
				}

				List<String> localizations = new ArrayList<>();
				int maxGBA = 0;
				for (String localization : gbaMap.keySet()) {
					int confidenceGBA = gbaMap.get(localization);
					if (confidenceGBA == maxGBA) {
						localizations.add(localization);
					} else if (confidenceGBA > maxGBA) {
						localizations.clear();
						localizations.add(localization);
						maxGBA = confidenceGBA;
					}
				}
				setLocalizations.put(node, localizations);
			} else
				setLocalizations.put(node, locals);
			List<String> localizations = setLocalizations.get(node);
			if (!localizations.isEmpty()) {
				int removeLocal = rand.nextInt(localizations.size());
				String localization = localizations.remove(removeLocal);
				localizeNode.setLocalization(localizer, localization);
			}
			if (localizations.isEmpty())
				setLocalizations.remove(node);
		}
		if (cloneNodes)
			insertTasksAfterCurrentTask(new CloneNodeTask(manager, network, setLocalizations, localizer));
	}
}