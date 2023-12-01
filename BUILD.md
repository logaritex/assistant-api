# GH Actions for Publishing to Maven Central

- [Publishing Artifacts to Maven Central using GitHub Actions: A Step-by-Step Guide](https://itnext.io/publishing-artifacts-to-maven-central-using-github-actions-a-step-by-step-guide-fd65ef075fd4)
  - [ghaction-import-gpg](https://github.com/crazy-max/ghaction-import-gpg)

- [How to Sign and Release to The Central Repository with GitHub Actions](https://gist.github.com/sualeh/ae78dc16123899d7942bc38baba5203c)
- [Publishing Java packages with Maven](https://docs.github.com/en/actions/publishing-packages/publishing-java-packages-with-maven) - lacking explanation how to add GPG signing.


## How to create a new GPG public/private pair.

- List all keys:

  ```bash
  gpg --list-secret-keys --keyid-format=long
  ```

- Save the public and private keys (for keyID: `XXX-KEY-ID-XXX`) into `logaritex.public.pgp` and `logaritex.private.pgp` files:

  ```bash
  gpg --output logaritex.public.pgp --armor --export XXX-KEY-ID-XXX
  gpg --output logaritex.private.pgp --armor --export-secret-key XXX-KEY-ID-XXX
  ```

- Send key to PGP server:

  ```bash
  gpg --keyserver hkp://keys.openpgp.org --send-keys XXX-KEY-ID-XXX
  ```

* Export your gpg private key from the system that you have created it.
  * Find your key-id (using `gpg --list-secret-keys --keyid-format=long`)
  * Export the gpg secret key to an ASCII file using `gpg --export-secret-keys -a <key-id> > secret.txt`
  * Edit `secret.txt` using a plain text editor, and replace all newlines with a literal "\n" until everything is on a single line:

    ```
    -----BEGIN PGP PRIVATE KEY BLOCK-----

    base64content (remove all \n in the content. But leave an empty line in front)
    -----END PGP PRIVATE KEY BLOCK-----
    ```

* Set up [GitHub Actions secrets](https://help.github.com/en/actions/configuring-and-managing-workflows/creating-and-storing-encrypted-secrets)
  * Create a secret called `MAVEN_GPG_PRIVATE_KEY` using the text from your edited secret.txt file (the whole text should be in a single line)
  * Create a secret called `MAVEN_GPG_PASSPHRASE` containing the passphrase for your gpg secret key.

## References:

* [Deploying to OSSRH with Apache Maven - Introduction](https://central.sonatype.org/publish/publish-maven/)
* [Publishing my first artifact to maven central using GitHub actions](https://theoverengineered.blog/posts/publishing-my-first-artifact-to-maven-central-using-github-actions)
* [Publishing Java packages with Maven](https://docs.github.com/en/actions/publishing-packages/publishing-java-packages-with-maven)
* [Publishing Github Java Packages to Maven Central for a New Domain](https://www.marcd.dev/articles/2021-03/mvncentral-publish-github)


* Nexus
  * https://s01.oss.sonatype.org/content/repositories/snapshots/com/logaritex/ai/assistant-api/
  * https://s01.oss.sonatype.org/#nexus-search;quick~logaritex