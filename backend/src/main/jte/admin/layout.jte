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
    <link rel="icon" href="/assets/favicon-32.png" type="image/png" sizes="32x32">
    <link rel="icon" href="/assets/favicon-32-white.png" type="image/png" sizes="32x32" media="(prefers-color-scheme: dark)">
    <link rel="apple-touch-icon" href="/assets/apple-touch-icon.png">
    <link rel="stylesheet" href="/css/styles.css">
    <link rel="stylesheet" href="/css/admin.css">
</head>
<body class="admin-body">
    <header class="admin-header">
        <div class="admin-header__left">
            <a class="logo" href="/admin">
                <img src="/assets/logo.svg" alt="Milastoria" width="120" height="20">
            </a>
            <nav class="admin-nav" aria-label="Админка">
                <a href="/admin">Лоты</a>
                <a href="/admin/categories">Разделы</a>
                <a href="/admin/watermark">Водяной знак</a>
                <a href="/admin/contacts">Контакты</a>
                <a href="/admin/other">Другое</a>
            </nav>
        </div>
        <form method="post" action="/admin/logout">
            <button type="submit" class="link" style="background:none;border:none;cursor:pointer;font:inherit;">Выйти</button>
        </form>
    </header>
    <main class="admin-main">
        ${content}
    </main>
    <script src="/js/admin.js"></script>
    <script src="/js/admin-dict.js"></script>
</body>
</html>
