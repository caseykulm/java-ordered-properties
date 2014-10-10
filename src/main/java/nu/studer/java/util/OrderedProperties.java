package nu.studer.java.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

/**
 * This class provides a drop-in replacement for the java.util.Properties class. It fixes the design flaw
 * of using inheritance over composition, while keeping up the same APIs as the original class. As additional
 * functionality, this class keeps its properties in a well-defined order. By default, the order is the one
 * in which the individual properties have been added, either through explicit API calls or through reading
 * them top-to-bottom from a properties file. Also, writing the comment that contains the current date when
 * storing the properties can be suppressed.
 * <p/>
 * This class is thread-safe.
 *
 * @see Properties
 */
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public final class OrderedProperties implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Object LOCK = new Object();

    private transient Map<String, String> properties;
    private transient boolean suppressDate;

    /**
     * Creates a new instance that will keep the properties in the order they have been added. Other than
     * the ordering of the keys, this instance behaves like an instance of the {@link Properties} class.
     */
    public OrderedProperties() {
        this(new LinkedHashMap<String, String>(), false);
    }

    private OrderedProperties(Map<String, String> properties, boolean suppressDate) {
        this.properties = properties;
        this.suppressDate = suppressDate;
    }

    /**
     * See {@link Properties#getProperty(String)}.
     */
    public String getProperty(String key) {
        synchronized (LOCK) {
            return properties.get(key);
        }
    }

    /**
     * See {@link Properties#getProperty(String, String)}.
     */
    public String getProperty(String key, String defaultValue) {
        synchronized (LOCK) {
            String value = properties.get(key);
            return (value == null) ? defaultValue : value;
        }
    }

    /**
     * See {@link Properties#setProperty(String, String)}.
     */
    public String setProperty(String key, String value) {
        synchronized (LOCK) {
            return properties.put(key, value);
        }
    }

    /**
     * See {@link Properties#isEmpty()}.
     */
    public boolean isEmpty() {
        synchronized (LOCK) {
            return properties.isEmpty();
        }
    }

    /**
     * See {@link Properties#propertyNames()}.
     */
    public Enumeration<?> propertyNames() {
        synchronized (LOCK) {
            return new Vector<String>(properties.keySet()).elements();
        }
    }

    /**
     * See {@link Properties#stringPropertyNames()}.
     */
    public Set<String> stringPropertyNames() {
        synchronized (LOCK) {
            return new LinkedHashSet<String>(properties.keySet());
        }
    }

    /**
     * See {@link Properties#load(InputStream)}.
     */
    public void load(InputStream stream) throws IOException {
        CustomProperties customProperties = new CustomProperties(this.properties);
        synchronized (LOCK) {
            customProperties.load(stream);
        }
    }

    /**
     * See {@link Properties#load(Reader)}.
     */
    public void load(Reader reader) throws IOException {
        CustomProperties customProperties = new CustomProperties(this.properties);
        synchronized (LOCK) {
            customProperties.load(reader);
        }
    }

    /**
     * See {@link Properties#loadFromXML(InputStream)}.
     */
    @SuppressWarnings("DuplicateThrows")
    public void loadFromXML(InputStream stream) throws IOException, InvalidPropertiesFormatException {
        CustomProperties customProperties = new CustomProperties(this.properties);
        synchronized (LOCK) {
            customProperties.loadFromXML(stream);
        }
    }

    /**
     * See {@link Properties#store(OutputStream, String)}.
     */
    public void store(OutputStream stream, String comments) throws IOException {
        CustomProperties customProperties = new CustomProperties(this.properties);
        synchronized (LOCK) {
            if (suppressDate) {
                customProperties.store(new DateSuppressingPropertiesBufferedWriter(new OutputStreamWriter(stream, "8859_1")), comments);
            } else {
                customProperties.store(stream, comments);
            }
        }
    }

    /**
     * See {@link Properties#store(Writer, String)}.
     */
    public void store(Writer writer, String comments) throws IOException {
        CustomProperties customProperties = new CustomProperties(this.properties);
        synchronized (LOCK) {
            if (suppressDate) {
                customProperties.store(new DateSuppressingPropertiesBufferedWriter(writer), comments);
            } else {
                customProperties.store(writer, comments);
            }
        }
    }

    /**
     * See {@link Properties#storeToXML(OutputStream, String)}.
     */
    public void storeToXML(OutputStream stream, String comment) throws IOException {
        CustomProperties customProperties = new CustomProperties(this.properties);
        synchronized (LOCK) {
            customProperties.storeToXML(stream, comment);
        }
    }

    /**
     * See {@link Properties#storeToXML(OutputStream, String, String)}.
     */
    public void storeToXML(OutputStream stream, String comment, String encoding) throws IOException {
        CustomProperties customProperties = new CustomProperties(this.properties);
        synchronized (LOCK) {
            customProperties.storeToXML(stream, comment, encoding);
        }
    }

    /**
     * See {@link Properties#toString()}.
     */
    @Override
    public String toString() {
        synchronized (LOCK) {
            return properties.toString();
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeObject(properties);
        stream.writeBoolean(suppressDate);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        properties = (Map<String, String>) stream.readObject();
        suppressDate = stream.readBoolean();
    }

    private void readObjectNoData() throws InvalidObjectException {
        throw new InvalidObjectException("Stream data required");
    }

    /**
     * Convert this instance to a {@link Properties} instance.
     *
     * @return the {@link Properties} instance
     */
    public Properties toJdkProperties() {
        Properties jdkProperties = new Properties();
        synchronized (LOCK) {
            for (Map.Entry<String, String> entry : this.properties.entrySet()) {
                jdkProperties.put(entry.getKey(), entry.getValue());
            }
        }
        return jdkProperties;
    }

    /**
     * Builder for {@link OrderedProperties} instances.
     */
    public static final class OrderedPropertiesBuilder {

        private Comparator<? super String> comparator;
        private boolean suppressDate;

        /**
         * Use a custom ordering of the keys.
         *
         * @param comparator the ordering to apply on the keys
         * @return the builder
         */
        public OrderedPropertiesBuilder withOrdering(Comparator<? super String> comparator) {
            this.comparator = comparator;
            return this;
        }

        /**
         * Suppress the comment that contains the current date when storing the properties.
         *
         * @param suppressDate whether to suppress the comment that contains the current date
         * @return the builder
         */
        public OrderedPropertiesBuilder suppressDateInComment(boolean suppressDate) {
            this.suppressDate = suppressDate;
            return this;
        }

        /**
         * Builds a new {@link OrderedProperties} instance.
         *
         * @return the new instance
         */
        public OrderedProperties build() {
            Map<String, String> properties = (this.comparator != null) ?
                    new TreeMap<String, String>(comparator) :
                    new LinkedHashMap<String, String>();
            return new OrderedProperties(properties, suppressDate);
        }

    }

    /**
     * Custom {@link Properties} that delegates reading, writing, and enumerating properties to the
     * backing {@link OrderedProperties} instance's properties.
     */
    private static final class CustomProperties extends Properties {

        private final Map<String, String> targetProperties;

        private CustomProperties(Map<String, String> targetProperties) {
            this.targetProperties = targetProperties;
        }

        @Override
        public Object get(Object key) {
            return targetProperties.get(key);
        }

        @Override
        public Object put(Object key, Object value) {
            return targetProperties.put((String) key, (String) value);
        }

        @Override
        public String getProperty(String key) {
            return targetProperties.get(key);
        }

        @Override
        public Enumeration<Object> keys() {
            return new Vector<Object>(targetProperties.keySet()).elements();
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public Set<Object> keySet() {
            return new LinkedHashSet<Object>(targetProperties.keySet());
        }

    }

    /**
     * Custom {@link BufferedWriter} for storing properties that will write all leading lines of comments except
     * the last comment line. Using the JDK Properties class to store properties, the last comment
     * line always contains the current date which is what we want to filter out.
     */
    private static final class DateSuppressingPropertiesBufferedWriter extends BufferedWriter {

        private final String LINE_SEPARATOR = System.getProperty("line.separator");

        private StringBuilder currentComment;
        private String previousComment;

        private DateSuppressingPropertiesBufferedWriter(Writer out) {
            super(out);
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public void write(String string) throws IOException {
            if (currentComment != null) {
                currentComment.append(string);
                if (string.endsWith(LINE_SEPARATOR)) {
                    if (previousComment != null) {
                        super.write(previousComment);
                    }

                    previousComment = currentComment.toString();
                    currentComment = null;
                }
            } else if (string.startsWith("#")) {
                currentComment = new StringBuilder(string);
            } else {
                super.write(string);
            }
        }

    }

}

