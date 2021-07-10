package com.project610;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
// NOTE: If you're using NanoHTTPD >= 3.0.0 the namespace is different,
//       instead of the above import use the following:
// import org.nanohttpd.NanoHTTPD;

public class Webserver extends NanoHTTPD {
    Janna janna;
    public Webserver(Janna janna) throws IOException {
        super(25610);
        this.janna = janna;
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("\nRunning! Point your browsers to http://localhost:25610/ \n");
    }

    @Override
    public Response serve(IHTTPSession session) {
        String msg = "<script type=\"text/javascript\">close();</script>";
        Map<String, List<String>> parms = session.getParameters();
        for (String key : parms.keySet()) {
            if (key.equalsIgnoreCase("token")) {
                janna.oauth = parms.get(key).get(0);
                System.out.println(janna.oauth);
            }
            for (String value : parms.get(key)) {
                System.out.println("Value: " + value);
            }
        }
        return newFixedLengthResponse(msg);
    }
}