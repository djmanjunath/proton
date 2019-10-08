package proton.core.workflow;

 

import javax.jcr.Node;
import javax.jcr.Session;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.adobe.granite.workflow.model.WorkflowNode;
import com.day.cq.commons.jcr.JcrConstants;
import java.util.Calendar;


@Component(service=WorkflowProcess.class, property = {"process.label=Proton Approve"})
public class Approve implements WorkflowProcess {

	private static final Logger log = LoggerFactory.getLogger(Approve.class);

	@Override
	public void execute(WorkItem item, WorkflowSession wrfSession, MetaDataMap metaDataMap) throws WorkflowException {

		Session session = wrfSession.adaptTo(Session.class);

		Calendar dataPublish = null;

		try {

			WorkflowNode wrkFlowNode = item.getNode();
			String wrkFlowTitle = wrkFlowNode.getTitle();
			log.debug("wrkFlowTitle {}", wrkFlowTitle);
			WorkflowData workflowData = item.getWorkflowData();
			String path = workflowData.getPayload().toString();

			log.debug("payload path {}", path);

			if (session != null) {
				Node JcrNode = session.getNode(path).getNode(JcrConstants.JCR_CONTENT);
				if (JcrNode != null && JcrNode.hasProperty("publishDate")) {
					dataPublish = JcrNode.getProperty("publishDate").getDate();
					log.debug("dataPublish is {}", dataPublish.getTimeInMillis());
					workflowData.getMetaDataMap().put("absoluteTime", dataPublish.getTimeInMillis());
				}
			}

		} catch (Exception e) {

			log.error("error {}", e);

		}

	}

}