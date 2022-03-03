/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ratis.thirdparty.demo.common;

import java.io.File;

/**
 * SSL client configurations.
 */
public class SslClientConfig extends SslConfig {
  private final File trustCertCollection;

  // Only needed for mTLS
  private final boolean mutualAuthn;
  private final File privateKey;
  private final File certChain;


  public SslClientConfig(File privateKey, File trustCertCollection, File certChain,
      boolean mutualAuthn, boolean encryption) {
    super(encryption);
    this.privateKey = privateKey;
    this.trustCertCollection = trustCertCollection;
    this.certChain = certChain;
    this.mutualAuthn = mutualAuthn;
  }

  public File getPrivateKey() {
    return privateKey;
  }

  public File getTrustCertCollection() {
    return trustCertCollection;
  }

  public File getCertChain() {
    return certChain;
  }

  public boolean isMutualAuthn() {
    return mutualAuthn && privateKey!= null && certChain!= null;
  }

}
