<html>
<body>
${kcSanitize(msg("memberUpdateUserInformBodyHtml", fullname, adminFullName, groupPath, validFrom, membershipExpiresAt, roles, signatureMessage))?no_esc}
</body>
</html>