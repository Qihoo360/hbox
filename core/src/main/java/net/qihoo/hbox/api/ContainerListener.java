package net.qihoo.hbox.api;

import net.qihoo.hbox.container.HboxContainerId;

public interface ContainerListener {

    void registerContainer(HboxContainerId hboxContainerId, String role);

    boolean isAllPsContainersFinished();

    boolean isTrainCompleted();

    boolean isAllWorkerContainersSucceeded();

    int interResultCompletedNum(Long lastInnerModel);

    boolean isAllContainerStarted();
}
