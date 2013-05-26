package net.floodlightcontroller.learningswitchoriginal;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;

public class LearningSwitchWebRoutableO implements RestletRoutable {

    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        router.attach("/table/{switch}/json", LearningSwitchTableO.class);
        return router;
    }

    @Override
    public String basePath() {
        return "/wm/learningswitch";
    }
}