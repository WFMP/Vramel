package com.nxttxn.vramel.components.gson;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Expression;
import com.nxttxn.vramel.builder.ExpressionBuilder;
import com.nxttxn.vramel.spi.DataFormat;
import com.nxttxn.vramel.util.IOHelper;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.LocalDate;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 4:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class GsonDataFormat implements DataFormat {
    private Expression unmarshalTypeExpression;
    private Gson gsonMarshaller;
    private Gson gsonUnmarshaller;

    private List<ExclusionStrategy> exclusionStrategies;
    private LongSerializationPolicy longSerializationPolicy;
    private FieldNamingPolicy fieldNamingPolicy;
    private FieldNamingStrategy fieldNamingStrategy;
    private Boolean serializeNulls;
    private Boolean prettyPrinting;
    private String dateFormatPattern;
    private Boolean polymorphic;

    public GsonDataFormat() throws Exception {
        this(ExpressionBuilder.constantExpression(Map.class));
    }

    /**
     * Use the default Gson {@link Gson} and with a custom
     * unmarshal type
     *
     * @param unmarshalTypeExpression the custom unmarshal type
     */
    public GsonDataFormat(Expression unmarshalTypeExpression) throws Exception {
        this(null, unmarshalTypeExpression);
    }


    /**
     * Use a custom Gson mapper and and unmarshal type
     *
     * @param gson          the custom mapper
     * @param unmarshalTypeExpression the custom unmarshal type
     */
    public GsonDataFormat(Gson gson, Expression unmarshalTypeExpression) throws Exception {
        this.gsonMarshaller = gson;
        this.unmarshalTypeExpression = unmarshalTypeExpression;
    }

    @Override
    public void marshal(Exchange exchange, Object graph, OutputStream stream) throws Exception {
        BufferedWriter writer = IOHelper.buffered(new OutputStreamWriter(stream));
        getGsonMarshaller().toJson(graph, writer);
        writer.close();
    }

    @Override
    public Object unmarshal(Exchange exchange, InputStream stream) throws Exception {
        Class unmarshalType = this.unmarshalTypeExpression.evaluate(exchange, Class.class);
        BufferedReader reader = IOHelper.buffered(new InputStreamReader(stream));

        Object result = getGsonUnmarshaller().fromJson(reader, unmarshalType);

        reader.close();
        return result;
    }

    protected GsonBuilder getGsonBuilderWithCommonAttributes() {
        return new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss")
                                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                                .registerTypeAdapter(LocalDate.class, new LocalDateTypeConverter())
                                .registerTypeAdapter(DateTime.class, new DateTimeTypeConverter())
                                .registerTypeAdapter(Instant.class, new InstantTypeConverter());
    }

    protected void buildDefaultGsonMarshaller() throws Exception {
        GsonBuilder builder = getGsonBuilderWithCommonAttributes();
        if (exclusionStrategies != null && !exclusionStrategies.isEmpty()) {
            ExclusionStrategy[] strategies = exclusionStrategies.toArray(new ExclusionStrategy[exclusionStrategies.size()]);
            builder.setExclusionStrategies(strategies);
        }
        if (longSerializationPolicy != null) {
            builder.setLongSerializationPolicy(longSerializationPolicy);
        }
        if (fieldNamingPolicy != null) {
            builder.setFieldNamingPolicy(fieldNamingPolicy);
        }
        if (fieldNamingStrategy != null) {
            builder.setFieldNamingStrategy(fieldNamingStrategy);
        }
        if (serializeNulls != null && serializeNulls) {
            builder.serializeNulls();
        }
        if (prettyPrinting != null && prettyPrinting) {
            builder.setPrettyPrinting();
        }
        if (dateFormatPattern != null) {
            builder.setDateFormat(dateFormatPattern);
        }
//        if (polymorphic != null && polymorphic) {
//            builder.registerTypeAdapter(unmarshalType, new InheritanceAdapter<Object>());
//        }
        gsonMarshaller =  builder.create();
    }


    // Properties
    // -------------------------------------------------------------------------

//    public Class<?> getUnmarshalType() {
//        return this.unmarshalType;
//    }
//
//    public void setUnmarshalType(Class<?> unmarshalType) {
//        this.unmarshalType = unmarshalType;
//    }

    public List<ExclusionStrategy> getExclusionStrategies() {
        return exclusionStrategies;
    }

    public void setExclusionStrategies(List<ExclusionStrategy> exclusionStrategies) {
        this.exclusionStrategies = exclusionStrategies;
    }

    public LongSerializationPolicy getLongSerializationPolicy() {
        return longSerializationPolicy;
    }

    public void setLongSerializationPolicy(LongSerializationPolicy longSerializationPolicy) {
        this.longSerializationPolicy = longSerializationPolicy;
    }

    public FieldNamingPolicy getFieldNamingPolicy() {
        return fieldNamingPolicy;
    }

    public void setFieldNamingPolicy(FieldNamingPolicy fieldNamingPolicy) {
        this.fieldNamingPolicy = fieldNamingPolicy;
    }

    public FieldNamingStrategy getFieldNamingStrategy() {
        return fieldNamingStrategy;
    }

    public void setFieldNamingStrategy(FieldNamingStrategy fieldNamingStrategy) {
        this.fieldNamingStrategy = fieldNamingStrategy;
    }

    public Boolean getSerializeNulls() {
        return serializeNulls;
    }

    public void setSerializeNulls(Boolean serializeNulls) {
        this.serializeNulls = serializeNulls;
    }

    public Boolean getPrettyPrinting() {
        return prettyPrinting;
    }

    public void setPrettyPrinting(Boolean prettyPrinting) {
        this.prettyPrinting = prettyPrinting;
    }

    public String getDateFormatPattern() {
        return dateFormatPattern;
    }

    public void setDateFormatPattern(String dateFormatPattern) {
        this.dateFormatPattern = dateFormatPattern;
    }

    public Gson getGsonMarshaller() throws Exception {
        if (gsonMarshaller == null) {
            buildDefaultGsonMarshaller();
        }
        return this.gsonMarshaller;
    }

    protected Gson getGsonUnmarshaller() {
        if ( gsonUnmarshaller == null) {
            gsonUnmarshaller = getGsonBuilderWithCommonAttributes().create();
        }
        return gsonUnmarshaller;
    }

    public void setPolymorphic(Boolean polymorphic) {
        this.polymorphic = polymorphic;
    }

    public Expression getUnmarshalTypeExpression() {
        return unmarshalTypeExpression;
    }

    public void setUnmarshalTypeExpression(Expression unmarshalTypeExpression) {
        this.unmarshalTypeExpression = unmarshalTypeExpression;
    }

    private class InheritanceAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T>{

        private static final String CLASSNAME = "CLASSNAME";
        private static final String INSTANCE  = "INSTANCE";

        @Override
        public JsonElement serialize(T src, Type typeOfSrc,
                                     JsonSerializationContext context) {

            JsonObject retValue = new JsonObject();
            String className = src.getClass().getCanonicalName();
            retValue.addProperty(CLASSNAME, className);
            JsonElement elem = context.serialize(src);
            retValue.add(INSTANCE, elem);
            return retValue;
        }

        @Override
        public T deserialize(JsonElement json, Type typeOfT,
                                   JsonDeserializationContext context) throws JsonParseException  {
            JsonObject jsonObject =  json.getAsJsonObject();
            JsonPrimitive prim = (JsonPrimitive) jsonObject.get(CLASSNAME);
            String className = prim.getAsString();

            Class<?> klass = null;
            try {
                klass = Class.forName(className);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw new JsonParseException(e.getMessage());
            }
            return context.deserialize(jsonObject.get(INSTANCE), klass);
        }

    }

    protected static class LocalDateTypeConverter implements JsonDeserializer<LocalDate>, JsonSerializer<LocalDate> {

        @Override
        public JsonElement serialize(LocalDate src, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public LocalDate deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            try {
                return new LocalDate(json.getAsString());
            } catch (IllegalArgumentException e) {
                try {
                    // Maybe this is in Date format
                    Date date = context.deserialize(json, Date.class);
                    return new LocalDate(date.getTime());
                } catch (JsonParseException p) {
                    throw new JsonParseException("Invalid date \""+json.getAsString()+"\"", e);
                }
            }
        }
    }

    protected static class DateTimeTypeConverter implements JsonSerializer<DateTime>, JsonDeserializer<DateTime> {
        @Override
        public JsonElement serialize(DateTime src, Type srcType, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public DateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                return new DateTime(json.getAsString());
            } catch (IllegalArgumentException e) {
                // May be it came in formatted as a java.util.Date, so try that
                Date date = context.deserialize(json, Date.class);
                return new DateTime(date);
            }
        }
    }

    protected static class InstantTypeConverter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override
        public JsonElement serialize(Instant src, Type srcType, JsonSerializationContext context) {
            return new JsonPrimitive(src.getMillis());
        }

        @Override
        public Instant deserialize(JsonElement json, Type type, JsonDeserializationContext context)
                throws JsonParseException {
            return new Instant(json.getAsLong());
        }
    }

}
