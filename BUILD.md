# Build tips

- https://itnext.io/publishing-artifacts-to-maven-central-using-github-actions-a-step-by-step-guide-fd65ef075fd4
- https://github.com/crazy-max/ghaction-import-gpg


- https://gist.github.com/sualeh/ae78dc16123899d7942bc38baba5203c
- https://docs.github.com/en/actions/publishing-packages/publishing-java-packages-with-maven


## GPG

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
  * Create a secret called `OSSRH_GPG_SECRET_KEY` using the text from your edited secret.txt file (the whole text should be in a single line)
  * Create a secret called `OSSRH_GPG_SECRET_KEY_PASSWORD` containing the password for your gpg secret key

## References:

* [Deploying to OSSRH with Apache Maven - Introduction](https://central.sonatype.org/publish/publish-maven/)
* [Publishing my first artifact to maven central using GitHub actions](https://theoverengineered.blog/posts/publishing-my-first-artifact-to-maven-central-using-github-actions)
* [Publishing Java packages with Maven](https://docs.github.com/en/actions/publishing-packages/publishing-java-packages-with-maven)
* [Publishing Github Java Packages to Maven Central for a New Domain](https://www.marcd.dev/articles/2021-03/mvncentral-publish-github)


* Nexus
  * https://s01.oss.sonatype.org/content/repositories/snapshots/com/logaritex/data/data-generator/
  * https://s01.oss.sonatype.org/#nexus-search;quick~logaritex