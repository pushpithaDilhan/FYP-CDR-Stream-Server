package com.fypcdr.app.stream.server;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.common.EntityStreamingSupport;
import akka.http.javadsl.common.JsonEntityStreamingSupport;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.unmarshalling.StringUnmarshallers;
import akka.stream.javadsl.Flow;
import akka.util.ByteString;

/**
 *
 * @author Lahiru Kaushalya
 */
public class Routes extends AllDirectives {

    final private LoggingAdapter log;
    final private CDRSource cdrSource;
            
    final ByteString start = ByteString.fromString("[");
    final ByteString between = ByteString.fromString(Settings.jsonSeparator);
    final ByteString end = ByteString.fromString("]");
    
    final Flow<ByteString, ByteString, NotUsed> compactArrayRendering =
      Flow.of(ByteString.class).intersperse(start, between, end);

    final JsonEntityStreamingSupport compactJsonSupport = EntityStreamingSupport.json()
      .withFramingRendererFlow(compactArrayRendering);
    
    public Routes(ActorSystem system) {
        this.log = Logging.getLogger(system, this);
        this.cdrSource = new CDRSource();
    }

    public Route routes() {
        return route(
            pathPrefix("cdrRecords", 
                () -> route(
                    getCDRRecords()
                )
            )
        );
    }

    private Route getCDRRecords() {
        return pathEnd(()
            -> withoutSizeLimit(() 
                -> route(
                    get(()
                        -> parameter(StringUnmarshallers.INTEGER, "start", _start
                        -> parameter(StringUnmarshallers.INTEGER, "end", _end
                        -> {
                            System.out.println("GET request received.\nStreaming CDR records from " + start + " to " + end);
                            return completeOKWithSource(cdrSource.getCDRSource(_start,_end), 
                                Jackson.marshaller(), 
                                compactJsonSupport
                            );
                        }))
                    )
                )
            )
        );
    }
}
