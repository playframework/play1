package play.libs;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CodecTest {

    @Test
    public void encodeBASE64() {
        // input and expected data are taken from https://datatracker.ietf.org/doc/html/rfc4648#section-10
        assertThat(Codec.encodeBASE64("")).isEmpty();
        assertThat(Codec.encodeBASE64(new byte[0])).isEmpty();

        assertThat(Codec.encodeBASE64("f")).isEqualTo("Zg==");
        assertThat(Codec.encodeBASE64("fo")).isEqualTo("Zm8=");
        assertThat(Codec.encodeBASE64("foo")).isEqualTo("Zm9v");
        assertThat(Codec.encodeBASE64("foob")).isEqualTo("Zm9vYg==");
        assertThat(Codec.encodeBASE64("fooba")).isEqualTo("Zm9vYmE=");
        assertThat(Codec.encodeBASE64("foobar")).isEqualTo("Zm9vYmFy");

        assertThat(Codec.encodeBASE64(new byte[] { 'f' })).isEqualTo("Zg==");
        assertThat(Codec.encodeBASE64(new byte[] { 'f', 'o' })).isEqualTo("Zm8=");
        assertThat(Codec.encodeBASE64(new byte[] { 'f', 'o', 'o' })).isEqualTo("Zm9v");
        assertThat(Codec.encodeBASE64(new byte[] { 'f', 'o', 'o', 'b' })).isEqualTo("Zm9vYg==");
        assertThat(Codec.encodeBASE64(new byte[] { 'f', 'o', 'o', 'b', 'a' })).isEqualTo("Zm9vYmE=");
        assertThat(Codec.encodeBASE64(new byte[] { 'f', 'o', 'o', 'b', 'a', 'r' })).isEqualTo("Zm9vYmFy");
    }

    @Test
    public void decodeBASE64() {
        // input and expected data are taken from https://datatracker.ietf.org/doc/html/rfc4648#section-10
        assertThat(Codec.decodeBASE64("")).isEmpty();
        assertThat(Codec.decodeBASE64("Zg==")).isEqualTo(new byte[] { 'f' });
        assertThat(Codec.decodeBASE64("Zm8=")).isEqualTo(new byte[] { 'f', 'o' });
        assertThat(Codec.decodeBASE64("Zm9v")).isEqualTo(new byte[] { 'f', 'o', 'o' });
        assertThat(Codec.decodeBASE64("Zm9vYg==")).isEqualTo(new byte[] { 'f', 'o', 'o', 'b' });
        assertThat(Codec.decodeBASE64("Zm9vYmE=")).isEqualTo(new byte[] { 'f', 'o', 'o', 'b', 'a' });
        assertThat(Codec.decodeBASE64("Zm9vYmFy")).isEqualTo(new byte[] { 'f', 'o', 'o', 'b', 'a', 'r' });
    }

    @Test
    public void hexMD5() {
        // https://www.febooti.com/products/filetweak/members/hash-and-crc/test-vectors/
        assertThat(Codec.hexMD5("")).isEqualTo("d41d8cd98f00b204e9800998ecf8427e");
        assertThat(Codec.hexMD5("The quick brown fox jumps over the lazy dog")).isEqualTo("9e107d9d372bb6826bd81d3542a419d6");
        assertThat(Codec.hexMD5("Test vector from febooti.com")).isEqualTo("500ab6613c6db7fbd30c62f5ff573d0f");
    }

    @Test
    public void hexSHA1() {
        // https://www.febooti.com/products/filetweak/members/hash-and-crc/test-vectors/
        assertThat(Codec.hexSHA1("")).isEqualTo("da39a3ee5e6b4b0d3255bfef95601890afd80709");
        assertThat(Codec.hexSHA1("The quick brown fox jumps over the lazy dog")).isEqualTo("2fd4e1c67a2d28fced849ee1bb76e7391b93eb12");
        assertThat(Codec.hexSHA1("Test vector from febooti.com")).isEqualTo("a7631795f6d59cd6d14ebd0058a6394a4b93d868");
    }

}
