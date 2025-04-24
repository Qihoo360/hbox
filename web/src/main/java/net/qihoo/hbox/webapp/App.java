package net.qihoo.hbox.webapp;

import com.google.inject.Inject;
import com.google.inject.servlet.RequestScoped;
import net.qihoo.hbox.api.ApplicationContext;

@RequestScoped
public class App {
    final ApplicationContext context;

    @Inject
    App(ApplicationContext context) {
        this.context = context;
    }
}
