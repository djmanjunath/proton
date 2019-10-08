package proton.core.workflow;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.AssetReferenceSearch;
import com.adobe.aemds.guide.utils.JcrResourceConstants;
import com.day.cq.tagging.Tag;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.adobe.granite.workflow.model.WorkflowNode;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentFilter;
import com.day.cq.replication.AgentManager;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.day.cq.tagging.TagManager;

@Component(service = WorkflowProcess.class, property = { "process.label=Replicate to Publish Process" })
@Designate(ocd = ReplicateProcess.PublishProcessConfig.class)
public class ReplicateProcess implements WorkflowProcess {

	private static final Logger log = LoggerFactory.getLogger(ReplicateProcess.class);

	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	@Reference
	private Replicator replicator;

	@Reference
	private AgentManager agentManager;

	private String ReplicateAgentName;

	Session session = null;

	@ObjectClassDefinition(name = "Publish Configuration", description = "Publish Configure Review ie. Publish agent name")
	public @interface PublishProcessConfig {

		@AttributeDefinition(name = "Publish replication agent name", description = "Configure Publish agent name")
		String getPublishReplicationAgentName() default "publish";
	}

	@Activate
	public void active(PublishProcessConfig publishProcessConfig) {

		log.debug("*********Active Method**************");
		ReplicateAgentName = publishProcessConfig.getPublishReplicationAgentName();
		log.debug("ReplicateAgentName is {}", ReplicateAgentName);
	}

	public void execute(WorkItem item, WorkflowSession wfsession, MetaDataMap args) throws WorkflowException {

		session = wfsession.adaptTo(Session.class);

		try {
			WorkflowNode wfNode = item.getNode();
			String title = wfNode.getTitle();
			log.debug("PreviewProcess Work flow title is {}", title);
			WorkflowData workflowData = item.getWorkflowData();
			String path = workflowData.getPayload().toString();
			log.debug("PreviewProcess Replication Path is {}", path);
			
			replicateContent(session, path);
			replicateAssets(session, path);
			replicateTags(session, path);

		}

		catch (Exception e) {
			log.debug("Error", e);
		}

	}

	/**
	 * 
	 * Search the Assets and replicate using the replication agent configuration.
	 *
	 * @param session
	 * @param contentPath
	 * @param replicationAgentArray
	 * @throws LoginException
	 * 
	 */

	private void replicateAssets(Session session, String contentPath) throws LoginException {

		try {

			final ResourceResolver resolver = getResourceResolver(session);
			final Node node = session.getNode(contentPath + "/" + JcrConstants.JCR_CONTENT);
			final AssetReferenceSearch assetReference = getAssetReferenceSearch(node, resolver);
			assetReference.search();
			final Map<String, Asset> assetReferences = new HashMap<>();
			assetReferences.putAll(assetReference.search());
			ReplicationOptions options = new ReplicationOptions();
			options.setSuppressStatusUpdate(true);
			options.setSynchronous(true);
			options.setSuppressStatusUpdate(false);
			options.setFilter(new AgentFilter() {

				public boolean isIncluded(final Agent agent) {
					return ReplicateAgentName.equals(agent.getId());
				}
			});

			for (Map.Entry<String, Asset> entry : assetReferences.entrySet()) {
				replicator.replicate(session, ReplicationActionType.ACTIVATE, entry.getKey(), options);
			}
		} catch (ReplicationException | RepositoryException e) {

			log.error("Error while replicating the assets", e);
		}

	}

	private void replicateContent(Session session, String contentPath) throws LoginException {

		try {

			ReplicationOptions options = new ReplicationOptions();
			options.setSuppressStatusUpdate(true);
			options.setSynchronous(true);
			options.setSuppressStatusUpdate(false);
			options.setFilter(new AgentFilter() {

				public boolean isIncluded(final Agent agent) {
					return ReplicateAgentName.contains(agent.getId());
				}

			});

			replicator.replicate(session, ReplicationActionType.ACTIVATE, contentPath, options);

		} catch (ReplicationException e) {

			log.error("Error while replicating the assets", e);
		}

	}

	private void replicateTags(Session session, String contentPath) throws LoginException {

		try {

			String[] tags = null;
			final ResourceResolver resolver = getResourceResolver(session);
			TagManager tagManager = resolver.adaptTo(TagManager.class);
			Resource jcrContentResource = resolver.getResource(contentPath + "/jcr:content");
			tags = jcrContentResource.getValueMap().get("cq:tags", String[].class);
			ReplicationOptions options = new ReplicationOptions();
			options.setSuppressStatusUpdate(true);
			options.setSynchronous(true);
			options.setSuppressStatusUpdate(false);
			options.setFilter(new AgentFilter() {

				public boolean isIncluded(final Agent agent) {
					return ReplicateAgentName.equals(agent.getId());
				}

			});

			if (tags != null) {
				for (String tag : tags) {

					com.day.cq.tagging.Tag pagetag = tagManager.resolve(tag);
					replicator.replicate(session, ReplicationActionType.ACTIVATE, pagetag.getPath(), options);
				}
			}

		} catch (ReplicationException e) {

			log.error("Error while replicating the assets", e);
		}

	}

	protected AssetReferenceSearch getAssetReferenceSearch(final Node node, final ResourceResolver resolver) {

		return new AssetReferenceSearch(node, DamConstants.MOUNTPOINT_ASSETS, resolver);
	}

	private ResourceResolver getResourceResolver(Session session) throws LoginException {

		return resourceResolverFactory.getResourceResolver(
				Collections.<String, Object>singletonMap(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, session));

	}

}