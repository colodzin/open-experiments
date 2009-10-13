package org.sakaiproject.kernel.proxy;

import org.apache.sling.api.SlingHttpServletRequest;
import org.sakaiproject.kernel.api.proxy.ProxyPreProcessor;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * 
 * @scr.service interface="org.sakaiproject.kernel.api.proxy.ProxyPreProcessor"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.component immediate="true" label="CamtoolsProxyPreProcessor"
 *                description="Pre processor for proxy requests to camtools" metatype="no"
 * @scr.property name="service.description"
 *               value="Pre processor for proxy requests to camtools."
 * 
 */
public class CamtoolsProxyPreProcessor implements ProxyPreProcessor {

  public String getName() {
    return "camtools";
  }

  public void preProcessRequest(SlingHttpServletRequest request,
      Map<String, String> headers, Map<String, Object> templateParams) {

    String user = request.getRemoteUser();
    //String secret = "The Snow on the Volga falls only under the bridges";
    String secret = "e2KS54H35j6vS5Z38nK40";
    String other = "" + System.currentTimeMillis();
    String hash = secret + ";" + user + ";" + other;
    try {
      hash = byteArrayToHexStr(MessageDigest.getInstance("SHA1").digest(
          hash.getBytes("UTF-8")));

    } catch (NoSuchAlgorithmException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    String full = hash + ";" + user + ";" + other;
    headers.put("X-SAKAI-TOKEN", full);
  }

  protected String byteArrayToHexStr(byte[] data) {
    char[] chars = new char[data.length * 2];
    for (int i = 0; i < data.length; i++) {
      byte current = data[i];
      int hi = (current & 0xF0) >> 4;
      int lo = current & 0x0F;
      chars[2 * i] = (char) (hi < 10 ? ('0' + hi) : ('A' + hi - 10));
      chars[2 * i + 1] = (char) (lo < 10 ? ('0' + lo) : ('A' + lo - 10));
    }
    return new String(chars);
  }

}
