/*
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package javax.xml.bind.annotation.adapters;

/**
 * Adapts a Java type for custom marshaling.
 * <p>
 * <p>
 * <b> Usage: </b>
 * </p>
 * <p>
 * <p>
 * Some Java types do not map naturally to a XML representation, for example
 * <tt>HashMap</tt> or other non JavaBean classes. Conversely, a XML
 * repsentation may map to a Java type but an application may choose to accesss
 * the XML representation using another Java type. For example, the schema to
 * Java binding rules bind xs:DateTime by default to XmlGregorianCalendar. But
 * an application may desire to bind xs:DateTime to a custom type,
 * MyXmlGregorianCalendar, for example. In both cases, there is a mismatch
 * between <i> bound type </i>, used by an application to access XML content and
 * the <i> value type</i>, that is mapped to an XML representation.
 * <p>
 * <p>
 * This abstract class defines methods for adapting a bound type to a value type
 * or vice versa. The methods are invoked by the JAXB binding framework during
 * marshaling and unmarshalling:
 * <p>
 * <ul>
 * <li><b> XmlAdapter.marshal(...): </b> During marshalling, JAXB binding
 * framework invokes XmlAdapter.marshal(..) to adapt a bound type to value type,
 * which is then marshaled to XML representation.</li>
 * <p>
 * <li><b> XmlAdapter.unmarshal(...): </b> During unmarshalling, JAXB binding
 * framework first unmarshals XML representation to a value type and then
 * invokes XmlAdapter.unmarshal(..) to adapt the value type to a bound type.
 * </li>
 * </ul>
 * <p>
 * Writing an adapter therefore involves the following steps:
 * <p>
 * <ul>
 * <li>Write an adapter that implements this abstract class.</li>
 * <li>Install the adapter using the annotation {@link XmlJavaTypeAdapter}</li>
 * </ul>
 * <p>
 * <p>
 * <b>Example:</b> Customized mapping of </tt>HashMap</tt>
 * </p>
 * <p>
 * The following example illustrates the use of <tt>&#64;XmlAdapter</tt> and
 * <tt>&#64;XmlJavaTypeAdapter</tt> to customize the mapping of a
 * <tt>HashMap</tt>.
 * <p>
 * <p>
 * <b> Step 1: </b> Determine the desired XML representation for HashMap.
 * <p>
 * <pre>
 *     &lt;hashmap>
 *         &lt;entry key="id123">this is a value&lt;/entry>
 *         &lt;entry key="id312">this is another value&lt;/entry>
 *         ...
 *       &lt;/hashmap>
 * </pre>
 * <p>
 * <p>
 * <b> Step 2: </b> Determine the schema definition that the desired XML
 * representation shown above should follow.
 * <p>
 * <pre>
 *
 *     &lt;xs:complexType name="myHashMapType">
 *       &lt;xs:sequence>
 *         &lt;xs:element name="entry" type="myHashMapEntryType"
 *                        minOccurs = "0" maxOccurs="unbounded"/>
 *       &lt;/xs:sequence>
 *     &lt;/xs:complexType>
 *
 *     &lt;xs:complexType name="myHashMapEntryType">
 *       &lt;xs:simpleContent>
 *         &lt;xs:extension base="xs:string">
 *           &lt;xs:attribute name="key" type="xs:int"/>
 *         &lt;/xs:extension>
 *       &lt;/xs:simpleContent>
 *     &lt;/xs:complexType>
 *
 * </pre>
 * <p>
 * <p>
 * <b> Step 3: </b> Write value types that can generate the above schema
 * definition.
 * <p>
 * <pre>
 * public class MyHashMapType {
 * 	List&lt;MyHashMapEntryType> entry;
 * }
 *
 * public class MyHashMapEntryType {
 * 	&#64;XmlAttribute
 * 	public Integer key;
 *
 * 	&#64;XmlValue
 * 	public String value;
 * }
 * </pre>
 * <p>
 * <p>
 * <b> Step 4: </b> Write the adapter that adapts the value type, MyHashMapType
 * to a bound type, HashMap, used by the application.
 * <p>
 * <pre>
 *     public final class MyHashMapAdapter extends
 *                        XmlAdapter&lt;MyHashMapType,HashMap> { ... }
 *
 * </pre>
 * <p>
 * <p>
 * <b> Step 5: </b> Use the adapter.
 * <p>
 * <pre>
 *     public class Foo {
 *         &#64;XmlJavaTypeAdapter(MyHashMapAdapter.class)
 *         HashMap hashmap;
 *         ...
 *     }
 * </pre>
 * <p>
 * The above code fragment will map to the following schema:
 * <p>
 * <pre>
 *     &lt;xs:complexType name="Foo">
 *       &lt;xs:sequence>
 *         &lt;xs:element name="hashmap" type="myHashMapType"
 *       &lt;/xs:sequence>
 *     &lt;/xs:complexType>
 * </pre>
 *
 * @param <BoundType> The type that JAXB doesn't know how to handle. An adapter is
 *                    written to allow this type to be used as an in-memory
 *                    representation through the <tt>ValueType</tt>.
 * @param <ValueType> The type that JAXB knows how to handle out of the box.
 * @see XmlJavaTypeAdapter
 * @since JAXB 2.0
 */
public abstract class XmlAdapter<ValueType, BoundType> {
    /**
     * Do-nothing constructor for the derived classes.
     */
    protected XmlAdapter() {
    }

    /**
     * Convert a value type to a bound type.
     *
     * @param v The value to be converted. Can be null.
     */
    public abstract BoundType unmarshal(ValueType v);

    /**
     * Convert a bound type to a value type.
     *
     * @param v The value to be convereted. Can be null.
     */
    public abstract ValueType marshal(BoundType v);
}