package com.fuint.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fuint.util.StringUtil;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * json反序列化日期转换
 *
 * Created by FSQ
 * Contact wx fsq_better
 * Site https://www.fuint.cn
 */
public class JsonDateDeserializer extends JsonDeserializer<Date> {

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {

        try {
            String fieldData = jsonParser.getText();
            if (StringUtil.isBlank(fieldData)) {
                return null;
            }
            return dateFormat.parse(fieldData);
        } catch (Exception e) {
            Calendar ca = Calendar.getInstance();
            ca.set(1970, Calendar.JANUARY, 1, 0, 0, 0);
            return ca.getTime();
        }
    }
}
