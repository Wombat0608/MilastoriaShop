@param String title
@param String description
@param String activeNav
@param gg.jte.Content content

<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title}</title>
    <meta name="description" content="${description}">
    <meta name="robots" content="index, follow">
    <link rel="icon" href="/assets/favicon.svg" type="image/svg+xml">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Fraunces:ital,opsz,wght@0,9..144,300;0,9..144,400;0,9..144,500;1,9..144,400&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="/css/styles.css">
</head>
<body>
    <div class="proto-banner">
        Milastoria · вёрстка перенесена на бэкенд (Javalin + jte + SQLite) · фото — рабочие кадры ателье
    </div>

    <header class="site-header" id="site-header">
        <div class="wrap site-header__inner">
            <a class="logo" href="/" aria-label="Milastoria — на главную">
                <img src="/assets/logo.svg" alt="Milastoria" width="140" height="24">
            </a>
            <nav class="nav" aria-label="Основная навигация">
                <a href="/" class="${activeNav.equals("home") ? "is-active" : ""}">Главная</a>
                <a href="/gallery" class="${activeNav.equals("gallery") ? "is-active" : ""}">Галерея</a>
                <a href="/gallery?category=family">Family Look</a>
                <a href="/#about">О нас</a>
                <a href="/#contacts">Контакты</a>
            </nav>
            <div class="header-contacts">
                <a href="tel:+79264296458">+7 (926) 429-64-58</a>
                <span class="header-contacts__sep" aria-hidden="true"></span>
                <a href="https://wa.me/79264296458" target="_blank" rel="noopener">WhatsApp</a>
            </div>
            <button class="nav-toggle" type="button" aria-label="Меню" aria-expanded="false" data-menu-toggle>
                <span></span><span></span><span></span>
            </button>
        </div>
    </header>

    <div class="mobile-menu" id="mobile-menu">
        <nav aria-label="Мобильная навигация">
            <a href="/">Главная</a>
            <a href="/gallery">Галерея</a>
            <a href="/gallery?category=family">Family Look</a>
            <a href="/#about">О нас</a>
            <a href="/#contacts">Контакты</a>
        </nav>
        <div class="header-contacts">
            <a href="tel:+79264296458">+7 (926) 429-64-58</a>
            <a href="https://wa.me/79264296458" target="_blank" rel="noopener">WhatsApp</a>
        </div>
    </div>

    <main>
        ${content}
    </main>

    <footer class="site-footer">
        <div class="wrap">
            <div class="footer-grid">
                <div>
                    <a class="logo" href="/">
                        <img src="/assets/logo.svg" alt="Milastoria" width="140" height="24">
                    </a>
                    <p style="margin-top:1.25rem; max-width:28em">
                        Ателье нарядного детского и женского платья. Галерея работ и пошив на заказ.
                    </p>
                </div>
                <div>
                    <h4>Разделы</h4>
                    <ul>
                        <li><a href="/gallery">Галерея</a></li>
                        <li><a href="/gallery?category=girls">Платья для девочек</a></li>
                        <li><a href="/gallery?category=women">Женское платье</a></li>
                        <li><a href="/gallery?category=family">Family Look</a></li>
                        <li><a href="/gallery?category=clients">Работы клиентов</a></li>
                    </ul>
                </div>
                <div>
                    <h4>Связь</h4>
                    <ul>
                        <li><a href="tel:+79264296458">+7 (926) 429-64-58</a></li>
                        <li><a href="mailto:shop@milastoria.ru">shop@milastoria.ru</a></li>
                        <li><span>Московская область, город Кубинка</span></li>
                    </ul>
                </div>
            </div>
            <div class="footer-bottom">
                <span>© Milastoria</span>
                <span>Кадрирование и водяные знаки — ImageMagick</span>
            </div>
        </div>
    </footer>

    <div class="lightbox" id="lightbox" role="dialog" aria-modal="true" aria-label="Просмотр изображения">
        <button type="button" class="lightbox__close" aria-label="Закрыть">×</button>
        <button type="button" class="lightbox__nav lightbox__nav--prev" aria-label="Предыдущее">‹</button>
        <img alt="">
        <button type="button" class="lightbox__nav lightbox__nav--next" aria-label="Следующее">›</button>
    </div>

    <script src="/js/site.js"></script>
</body>
</html>
