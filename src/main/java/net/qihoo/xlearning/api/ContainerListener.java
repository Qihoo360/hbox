package net.qihoo.xlearning.api;

import net.qihoo.xlearning.container.XLearningContainerId;

public interface ContainerListener {

  void registerContainer(XLearningContainerId xlearningContainerId, String role);

  boolean isAllPsContainersFinished();

  boolean isTrainCompleted();

  boolean isAllWorkerContainersSucceeded();

  int interResultCompletedNum(Long lastInnerModel);

  boolean isAllContainerStarted();
}
