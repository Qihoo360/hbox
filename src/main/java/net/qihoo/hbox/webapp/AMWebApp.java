/**
 *
 */
package net.qihoo.hbox.webapp;

import org.apache.hadoop.yarn.webapp.WebApp;

public class AMWebApp extends WebApp implements AMParams {

    @Override
    public void setup() {
        bind(AMWebServices.class);
        route("/", AppController.class);
        route("/savedmodel", AppController.class, "savedmodel");
    }
}