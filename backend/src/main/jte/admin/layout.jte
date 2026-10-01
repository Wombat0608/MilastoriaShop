@param String title
@param gg.jte.Content content

<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title} — Milastoria admin</title>
    <meta name="robots" content="noindex, nofollow">
    <link rel="icon" href="/assets/favicon.svg" type="image/svg+xml">
    <link rel="stylesheet" href="/css/styles.css">
    <link rel="stylesheet" href="/css/admin.css">
</head>
<body class="admin-body">
    <header class="admin-header">
        <a class="logo" href="/admin">
            <img src="/assets/logo.svg" alt="Milastoria" width="120" height="20">
        </a>
        <form method="post" action="/admin/logout">
            <button type="submit" class="link" style="background:none;border:none;cursor:pointer;font:inherit;">Выйти</button>
        </form>
    </header>
    <main class="admin-main">
        ${content}
    </main>
</body>
</html>
