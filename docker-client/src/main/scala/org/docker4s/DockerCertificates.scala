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
package org.docker4s

import java.io.{FileInputStream, InputStreamReader}
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

case class DockerCertificates(sslContext: SSLContext)

object DockerCertificates extends LazyLogging {

  private val DEFAULT_CA_CERT_NAME = "ca.pem"
  private val DEFAULT_CLIENT_CERT_NAME = "cert.pem"
  private val DEFAULT_CLIENT_KEY_NAME = "key.pem"

  private val KEY_STORE_PASSWORD = "docker-secret-123".toCharArray

  Security.addProvider(new BouncyCastleProvider)

  def apply(certificatePath: Path): DockerCertificates = apply(
    certificatePath.resolve(DEFAULT_CLIENT_KEY_NAME),
    certificatePath.resolve(DEFAULT_CLIENT_CERT_NAME),
    certificatePath.resolve(DEFAULT_CA_CERT_NAME)
  )

  def apply(clientKey: Path, clientCert: Path, caCert: Path): DockerCertificates = apply(
    clientKey = loadPrivateKey(clientKey),
    clientCerts = loadCertificates(clientCert),
    caCerts = loadCertificates(caCert)
  )

  def apply(clientKey: PrivateKey, clientCerts: Seq[Certificate], caCerts: Seq[Certificate]): DockerCertificates = {
    val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
    keyManagerFactory.init(createKeyStore(clientKey, clientCerts), KEY_STORE_PASSWORD)

    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    trustManagerFactory.init(createTrustStore(caCerts))

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, null)

    DockerCertificates(sslContext)
  }

  private def createKeyStore(privateKey: Path, cert: Path): KeyStore =
    createKeyStore(loadPrivateKey(privateKey), loadCertificates(cert))

  private def createKeyStore(privateKey: PrivateKey, certificates: Seq[Certificate]): KeyStore = {
    val keyStore = KeyStore.getInstance("JKS")
    keyStore.load(null)
    keyStore.setKeyEntry("docker", privateKey, KEY_STORE_PASSWORD, certificates.toArray)
    keyStore
  }

  private def createTrustStore(caCerts: Path): KeyStore = createTrustStore(loadCertificates(caCerts))

  private def createTrustStore(caCerts: Seq[Certificate]): KeyStore = {
    val trustStore = KeyStore.getInstance("JKS")
    trustStore.load(null)

    caCerts.zipWithIndex.foreach({
      case (certificate, index) =>
        trustStore.setCertificateEntry("ca-" + index, certificate)
    })

    trustStore
  }

  private def loadPrivateKey(path: Path): PrivateKey = {
    loadPem(path)
      .flatMap({
        case key: PEMKeyPair     => Some(key.getPrivateKeyInfo)
        case key: PrivateKeyInfo => Some(key)
        case key =>
          logger.info(s"Unknown private key info $key in path $path.")
          None
      })
      .map({ key =>
        new JcaPEMKeyConverter().getPrivateKey(key)
      })
      .headOption
      .getOrElse(
        throw new IllegalArgumentException(s"Cannot load a private key from $path.")
      )
  }

  /**
    * Loads all certificates available in a given PEM file.
    */
  private def loadCertificates(path: Path): Seq[Certificate] = {
    val converter = new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
    loadPem(path)
      .flatMap({
        case certificate: X509CertificateHolder =>
          Some(converter.getCertificate(certificate))

        case certificate =>
          logger.info(s"Unknown certificate holder $certificate in path $path.")
          None
      })
  }

  /**
    * Loads all PEM objects from the given file.
    */
  private def loadPem(path: Path): Seq[Object] = {
    val parser = new PEMParser(new InputStreamReader(new FileInputStream(path.toFile), Charset.defaultCharset()))
    try {
      @tailrec
      def go(acc: Seq[Object]): Seq[Object] = {
        parser.readObject() match {
          case null => acc
          case obj  => go(acc :+ obj)
        }
      }

      go(Seq.empty)
    } finally {
      parser.close()
    }
  }

}
