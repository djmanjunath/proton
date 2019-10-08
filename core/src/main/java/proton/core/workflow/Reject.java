package proton.core.workflow;

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

@Component(service = WorkflowProcess.class, property = { "process.label=Proton Reject" })
public class Reject implements WorkflowProcess {

	private static final Logger log = LoggerFactory.getLogger(Reject.class);

	@Override
	public void execute(WorkItem item, WorkflowSession wrfSession, MetaDataMap metaDataMap) throws WorkflowException {

		try {

			WorkflowNode wrkFlowNode = item.getNode();
			String wrkFlowTitle = wrkFlowNode.getTitle();
			log.debug("wrkFlowTitle {}", wrkFlowTitle);
			WorkflowData workflowData = item.getWorkflowData();
			String path = workflowData.getPayload().toString();
			log.debug("payload path {}", path);

		} catch (Exception e) {
			log.error("error {}", e);
		}

	}

}
