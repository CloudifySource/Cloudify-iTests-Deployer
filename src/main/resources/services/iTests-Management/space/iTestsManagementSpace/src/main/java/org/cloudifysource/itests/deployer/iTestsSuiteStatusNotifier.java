package org.cloudifysource.itests.deployer;

import com.gigaspaces.document.SpaceDocument;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.notify.Notify;
import org.openspaces.events.notify.NotifyBatch;

/**
 * User: Sagi Bernstein
 * Date: 08/04/13
 * Time: 13:24
 */
@EventDriven
@Notify
@NotifyBatch(size = 10, time = 5000)
public class iTestsSuiteStatusNotifier {

    @EventTemplate
    SpaceDocument unprocessedData() {
        SpaceDocument template = new SpaceDocument();
        template.setTypeName("TestSuiteStatus");
        return template;
    }

    @SpaceDataEvent
    public SpaceDocument eventListener(SpaceDocument event) {
        return event;
    }

}
