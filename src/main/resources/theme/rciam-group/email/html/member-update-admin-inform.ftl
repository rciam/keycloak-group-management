<html>
<body>
${kcSanitize(msg("memberUpdateAdminInformBodyHtml", fullname, adminFullName, userFullName, groupPath, validFrom, membershipExpiresAt, roles, signatureMessage))?no_esc}
</body>
</html>