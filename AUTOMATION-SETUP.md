# Daily update setup

This version searches PubMed every day and saves new records as hidden review candidates. Pending records are never shown in the public clinical-update list.

## One-time setup

1. Put these website files in the GitHub repository connected to Netlify.
2. In the GitHub repository, open **Settings → Secrets and variables → Actions**.
3. Add `NCBI_EMAIL` as a repository variable. Use the email address responsible for this automated NCBI client.
4. Optionally add `NCBI_API_KEY` as a repository secret for higher NCBI request limits.
5. Open **Actions → Daily medical update review → Run workflow** to test it.

## Daily review

When a GitHub issue titled **Review pending gastroenterology updates** appears:

1. Open `data/updates.json` in the repository.
2. Select the pencil button to edit the file.
3. Verify every candidate using its `sourceUrl` PubMed link.
4. Delete irrelevant candidate objects.
5. Change `"reviewStatus": "pending"` to `"reviewStatus": "approved"` only for verified items.
6. Select **Commit changes** and commit directly to the default branch.

Netlify will deploy the approved changes automatically when it is connected to the repository.

## Notifications

### GitHub email notifications

1. In GitHub, open the repository and select **Watch → Custom**.
2. Enable **Issues** and **Actions**, then save.
3. Open **GitHub profile photo → Settings → Notifications**.
4. Under **Watching**, enable email delivery and confirm that your email address is verified.

The automation opens or refreshes an issue titled **Review pending gastroenterology updates** whenever candidates are waiting.

### Website bell

1. Open the deployed site.
2. Select the bell in the upper-right corner.
3. Choose **Allow** when the browser asks for notification permission.

The bell shows the number of pending candidates and displays a browser notification when the site is open and a new pending set is detected. GitHub email is the reliable alert when the site is closed.

## Important limitation

The automation retrieves bibliographic data and abstracts. It does not interpret an abstract as a clinical guideline or replace specialist review. Do not add treatment recommendations unless they are directly supported by the cited source.
