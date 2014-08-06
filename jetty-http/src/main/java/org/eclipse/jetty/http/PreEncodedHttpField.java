//
//  ========================================================================
//  Copyright (c) 1995-2014 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//


package org.eclipse.jetty.http;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;


/* ------------------------------------------------------------ */
/** Pre encoded HttpField.
 * <p>A HttpField that will be cached and used many times can be created as 
 * a {@link PreEncodedHttpField}, which will use the {@link HttpFieldPreEncoder}
 * instances discovered by the {@link ServiceLoader} to pre-encode the header 
 * for each version of HTTP in use.  This will save garbage 
 * and CPU each time the field is encoded into a response.
 * </p>
 */
public class PreEncodedHttpField extends HttpField
{
    private final static Logger LOG = Log.getLogger(PreEncodedHttpField.class);
    private final static HttpFieldPreEncoder[] __encoders;
    
    static
    { 
        List<HttpFieldPreEncoder> encoders = new ArrayList<>();
        for (HttpFieldPreEncoder enc : ServiceLoader.load(HttpFieldPreEncoder.class,PreEncodedHttpField.class.getClassLoader()))
            encoders.add(enc);
        LOG.debug("HttpField encoders loaded: {}",encoders);
        __encoders = encoders.toArray(new HttpFieldPreEncoder[encoders.size()]);
    }
    
    private final byte[][] _encodedField=new byte[2][];

    public PreEncodedHttpField(HttpHeader header,String name,String value)
    {
        super(header,name, value);
        
        for (HttpFieldPreEncoder e:__encoders)
        {
            _encodedField[e.getHttpVersion()==HttpVersion.HTTP_2?1:0]=e.getEncodedField(header,header.asString(),value);
        }
    }
    
    public PreEncodedHttpField(HttpHeader header,String value)
    {
        this(header,header.asString(),value);
    }
    
    public PreEncodedHttpField(String name,String value)
    {
        this(null,name,value);
    }
    
    public void putTo(ByteBuffer bufferInFillMode, HttpVersion version)
    {
        bufferInFillMode.put(_encodedField[version==HttpVersion.HTTP_2?1:0]);
    }
}