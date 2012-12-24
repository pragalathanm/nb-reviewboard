/*
 * Created on: Mar 6, 2012
 */
package org.smartdevelop.reviewboard.client.response;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

/**
 *
 * @author Pragalathan M
 */
public class JsonDateSerializer extends JsonDeserializer<Date> {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public Date deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
        try {
            return dateFormat.parse(jp.getText());
        } catch (ParseException ex) {
            Logger.getLogger(ReviewRequestResponse.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
