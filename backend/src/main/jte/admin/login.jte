@param String error

<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Вход — Milastoria admin</title>
    <meta name="robots" content="noindex, nofollow">
    <link rel="icon" href="/assets/favicon.svg" type="image/svg+xml">
    <link rel="icon" href="/assets/favicon-32.png" type="image/png" sizes="32x32">
    <link rel="icon" href="/assets/favicon-32-white.png" type="image/png" sizes="32x32" media="(prefers-color-scheme: dark)">
    <link rel="apple-touch-icon" href="/assets/apple-touch-icon.png">
    <link rel="stylesheet" href="/css/styles.css">
    <link rel="stylesheet" href="/css/admin.css">
</head>
<body class="admin-body">
    <main class="admin-main">
        <div class="admin-login">
            <a class="logo" href="/" style="display:block; margin-bottom:2rem;">
                <img src="/assets/logo.svg" alt="Milastoria" width="140" height="24">
            </a>
            <h1>Вход в админку</h1>
            @if(error != null)
                <p class="admin-error">${error}</p>
            @endif
            <form method="post" action="/admin/login" class="admin-form">
                <label>
                    Логин
                    <input type="text" name="username" autocomplete="username" required autofocus>
                </label>
                <label>
                    Пароль
                    <input type="password" name="password" autocomplete="current-password" required>
                </label>
                <button type="submit" class="btn btn--fill">Войти</button>
            </form>
        </div>
    </main>
</body>
</html>
