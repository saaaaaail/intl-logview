package com.iqiyi.intl.logview.util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 各种格式的编码加码工具类.
 * <p>
 * 集成Commons-Codec,Commons-Lang及JDK提供的编解码方法.
 */
public class EncodeUtils {
    private static final Logger logger = LoggerFactory.getLogger(EncodeUtils.class);
    private static final String DEFAULT_URL_ENCODING = "UTF-8";
    private static final String HMAC_SHA1 = "HmacSHA1";

    /**
     * Hex编码.
     *
     * @param input 需要编码的字节数组
     * @return 编码后的字符串
     */
    public static String hexEncode(byte[] input) {
        return Hex.encodeHexString(input);
    }

    /**
     * Hex解码.
     *
     * @param input 需要解码的字符串
     * @return 转码后的字节数组
     */
    public static byte[] hexDecode(String input) {
        try {
            return Hex.decodeHex(input.toCharArray());
        } catch (DecoderException e) {
            throw new IllegalStateException("Hex Decoder exception", e);
        }
    }

    /**
     * Base64编码.
     *
     * @param input 需要编码的字节数组
     * @return 编码后的字符串
     */
    public static String base64Encode(byte[] input) {
        return new String(Base64.encodeBase64(input));
    }

    /**
     * Base64编码, URL安全(将Base64中的URL非法字符如+,/=转为其他字符, 见RFC3548).
     *
     * @param input 需要编码的字节数组
     * @return 编码后的字符串
     */
    public static String base64UrlSafeEncode(byte[] input) {
        return Base64.encodeBase64URLSafeString(input);
    }

    /**
     * Base64解码.
     *
     * @param input 需要解码的字符串
     * @return 转码后的字节数组
     */
    public static byte[] base64Decode(String input) {
        return Base64.decodeBase64(input);
    }

    /**
     * URL 编码, Encode默认为UTF-8.
     *
     * @param input 需要编码的字节数组
     * @return 编码后的字符串
     */
    public static String urlEncode(String input) {
        try {
            return URLEncoder.encode(input, DEFAULT_URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Unsupported Encoding Exception", e);
        }
    }

    /**
     * URL 解码, Encode默认为UTF-8.
     *
     * @param input 需要解码的字符串
     * @return 转码后的字节数组
     */
    public static String urlDecode(String input) {
        try {
            return URLDecoder.decode(input, DEFAULT_URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Unsupported Encoding Exception", e);
        }
    }

    /**
     * Html 转码.
     *
     * @param html 需要编码的字节数组
     * @return 编码后的字符串
     */
    public static String htmlEscape(String html) {
        return StringEscapeUtils.escapeHtml4(html);
    }

    /**
     * Html 解码.
     *
     * @param htmlEscaped 需要编码的字节数组
     * @return 编码后的字符串
     */
    public static String htmlUnescape(String htmlEscaped) {
        return StringEscapeUtils.unescapeHtml4(htmlEscaped);
    }

    /**
     * Xml 转码.
     *
     * @param xml 需要编码的字节数组
     * @return 编码后的字符串
     */
    public static String xmlEscape(String xml) {
        return StringEscapeUtils.escapeXml(xml);
    }

    /**
     * Xml 解码.
     *
     * @param xmlEscaped 需要编码的字节数组
     * @return 编码后的字符串
     */
    public static String xmlUnescape(String xmlEscaped) {
        return StringEscapeUtils.unescapeXml(xmlEscaped);
    }


    private static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * 按指定编码对字符串进行加密, 默认UTF-8
     *
     * @param text    需要加密的文本
     * @param charset 加密的编码格式
     * @return 加密串
     */
    public static String MD5(String text, String charset) {
        MessageDigest msgDigest = null;

        try {
            msgDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "System doesn't support MD5 algorithm.");
        }

        try {
            msgDigest.update(text.getBytes(charset));    //注意改接口是按照指定编码形式签名

        } catch (UnsupportedEncodingException e) {

            throw new IllegalStateException(
                    "System doesn't support your  EncodingException.");

        }

        byte[] bytes = msgDigest.digest();

        String md5Str = new String(encodeHex(bytes));

        return md5Str;
    }


    /**
     * 按指定编码对字符串进行加密, 默认UTF-8
     *
     * @param text    需要加密的文本
     * @param charset 加密的编码格式
     * @return 加密串
     */
    public static String SHA256(String text, String charset) {
        MessageDigest msgDigest = null;

        try {
            msgDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "System doesn't support MD5 algorithm.");
        }

        try {
            msgDigest.update(text.getBytes(charset));    //注意改接口是按照指定编码形式签名

        } catch (UnsupportedEncodingException e) {

            throw new IllegalStateException(
                    "System doesn't support your  EncodingException.");

        }

        byte[] bytes = msgDigest.digest();

        String md5Str = new String(encodeHex(bytes));

        return md5Str;
    }

    public static char[] encodeHex(byte[] data) {

        int l = data.length;

        char[] out = new char[l << 1];

        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS[0x0F & data[i]];
        }

        return out;
    }

    /**
     * HMACSHA1加密
     */
    public static String hmacsha1(String data, String key) {
        byte[] keyBytes = key.getBytes();
        SecretKeySpec signingKey = new SecretKeySpec(keyBytes, HMAC_SHA1);
        Mac mac = null;
        try {
            mac = Mac.getInstance(HMAC_SHA1);
            mac.init(signingKey);
        } catch (Exception e) {
            logger.error("[module:Mac] [action:EncodeUtils] [step:hmacsha1] ", e);
        }
        byte[] rawHmac = mac.doFinal(data.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : rawHmac) {
            sb.append(byteToHexString(b));
        }
        return sb.toString();
    }

    private static String byteToHexString(byte ib) {
        char[] ob = new char[2];
        ob[0] = DIGITS[(ib >>> 4) & 0X0f];
        ob[1] = DIGITS[ib & 0X0F];
        String s = new String(ob);
        return s;
    }

    /*public static void main(String[] args){
        String data="feed_vipindex";
        String key="myappsecret&";
        System.out.println(EncodeUtils.MD5("b16ec69a9978324e1234560cd0c3ca5b81422fa772c137a1c483c5MwI0OGckemR5NzzxMjMxMzI0pjMxMzI0","UTF-8"));
		System.out.println(EncodeUtils.MD5("qiyue_expcard_1qaz!2wsx@3edc#kevin4RFV$5TGB%6YHN", "UTF-8"));
	}*/
}
