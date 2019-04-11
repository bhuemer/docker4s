/*
 * Copyright (c) 2019 Bernhard Huemer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.docker4s.util

import java.io._
import java.nio.charset.Charset
import java.nio.file.Path
import java.security.{KeyStore, PrivateKey, Security}
import java.security.cert.Certificate

import com.typesafe.scalalogging.LazyLogging
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.{PEMKeyPair, PEMParser}

import scala.annotation.tailrec

/**
  * Utility methods wrt/ loading certificates/private keys and creating an SSL context for the transport layer.
  */
object Certificates extends LazyLogging {

  private val KEY_STORE_PASSWORD = "docker-secret-123".toCharArray

  Security.addProvider(new BouncyCastleProvider)

  trait PemFile {

    /**
      * Loads all PEM objects from this file.
      */
    def load: Seq[Object]

    /**
      * Attempts to load a private key from this PEM file.
      */
    def loadPrivateKey: PrivateKey = {
      load
        .flatMap({
          case key: PEMKeyPair     => Some(key.getPrivateKeyInfo)
          case key: PrivateKeyInfo => Some(key)
          case key =>
            logger.info(s"Unknown private key info $key (in $this).")
            None
        })
        .map({ key =>
          new JcaPEMKeyConverter().getPrivateKey(key)
        })
        .headOption
        .getOrElse(
          throw new IllegalArgumentException(s"Cannot load a private key from this file ($this) - none found.")
        )
    }

    /**
      * Attempts to load all  509 certificates available in this PEM file.
      */
    def loadCertificates: Seq[Certificate] = {
      val converter = new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
      load.flatMap({
        case certificate: X509CertificateHolder =>
          Some(converter.getCertificate(certificate))

        case certificate =>
          logger.info(s"Unknown certificate holder $certificate (in $this).")
          None
      })
    }

  }

  object PemFile {

    def apply(path: Path, charset: Charset = Charset.defaultCharset()): PemFile = new PemFile {
      override def load: Seq[Object] =
        loadAll(new InputStreamReader(new FileInputStream(path.toFile), charset), name = s"$path")

      // Makes sure that logging statements are more useful
      override def toString: String = s"PemFile[path: $path]"
    }

    def apply(name: String, contents: String): PemFile = new PemFile {
      override def load: Seq[Object] = loadAll(new StringReader(contents), name)
      override def toString: String = s"PemFile[name: $name]"
    }

    /**
      * Utility methods that loads all PEM objects from the given reader. Closes the reader at the end.
      */
    private def loadAll(reader: Reader, name: => String): Seq[Object] = {
      val parser = new PEMParser(reader)
      try {
        @tailrec
        def go(acc: Seq[Object]): Seq[Object] = {
          parser.readObject() match {
            case null => acc
            case obj  => go(acc :+ obj)
          }
        }

        go(Seq.empty)
      } catch {
        case ex: IOException =>
          throw new IOException(s"Error occurred while trying to read $name.", ex)
      } finally {
        parser.close()
      }
    }

  }

  def createSslContext(clientKey: Path, clientCerts: Path, caCerts: Path): SSLContext =
    createSslContext(PemFile(clientKey), PemFile(clientCerts), PemFile(caCerts))

  def createSslContext(clientKey: PemFile, clientCerts: PemFile, caCerts: PemFile): SSLContext =
    createSslContext(clientKey.loadPrivateKey, clientCerts.loadCertificates, caCerts.loadCertificates)

  def createSslContext(clientKey: PrivateKey, clientCerts: Seq[Certificate], caCerts: Seq[Certificate]): SSLContext = {
    val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
    keyManagerFactory.init(Certificates.createKeyStore(clientKey, KEY_STORE_PASSWORD, clientCerts), KEY_STORE_PASSWORD)

    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    trustManagerFactory.init(Certificates.createTrustStore(caCerts))

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, null)
    sslContext
  }

  def createKeyStore(privateKey: Path, password: Array[Char], certificates: Path): KeyStore =
    createKeyStore(PemFile(privateKey), password, PemFile(certificates))

  def createKeyStore(privateKey: PemFile, password: Array[Char], certificates: PemFile): KeyStore =
    createKeyStore(privateKey.loadPrivateKey, password, certificates.loadCertificates)

  def createKeyStore(privateKey: PrivateKey, password: Array[Char], certificates: Seq[Certificate]): KeyStore = {
    val keyStore = KeyStore.getInstance("JKS")
    keyStore.load(null)
    keyStore.setKeyEntry("key", privateKey, password, certificates.toArray)
    keyStore
  }

  def createTrustStore(caCerts: Path): KeyStore = createTrustStore(PemFile(caCerts))

  def createTrustStore(pemFile: PemFile): KeyStore = createTrustStore(pemFile.loadCertificates)

  def createTrustStore(caCerts: Seq[Certificate]): KeyStore = {
    val trustStore = KeyStore.getInstance("JKS")
    trustStore.load(null)

    caCerts.zipWithIndex.foreach({
      case (certificate, index) =>
        trustStore.setCertificateEntry("ca-" + index, certificate)
    })

    trustStore
  }

}
