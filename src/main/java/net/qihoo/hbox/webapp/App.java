package net.qihoo.hbox.webapp;

import net.qihoo.hbox.api.ApplicationContext;

import com.google.inject.Inject;
import com.google.inject.servlet.RequestScoped;

@RequestScoped
public class App {
  final ApplicationContext context;

  @Inject
  App(ApplicationContext context) {
    this.context = context;
  }
}
