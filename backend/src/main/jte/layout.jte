@param String title
@param String description
@param String activeNav
@param ru.milastoria.view.SiteContacts contacts
@param String baseUrl
@param String path
@param String extraJsonLd
@param gg.jte.Content content

<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title}</title>
    <meta name="description" content="${description}">
    <meta name="robots" content="index, follow">
    <meta name="author" content="Milastoria">
    <meta name="theme-color" content="#f6f0e6">
    <link rel="canonical" href="${ru.milastoria.view.SiteSeo.absolute(baseUrl, path)}">
    <link rel="icon" href="/assets/favicon.svg" type="image/svg+xml">
    <link rel="icon" href="/assets/favicon-32.png" type="image/png" sizes="32x32">
    <link rel="icon" href="/assets/favicon-32-white.png" type="image/png" sizes="32x32" media="(prefers-color-scheme: dark)">
    <link rel="apple-touch-icon" href="/assets/apple-touch-icon.png">

    <!-- Open Graph / Telegram / VK -->
    <meta property="og:type" content="website">
    <meta property="og:site_name" content="Milastoria">
    <meta property="og:locale" content="ru_RU">
    <meta property="og:title" content="${title}">
    <meta property="og:description" content="${description}">
    <meta property="og:url" content="${ru.milastoria.view.SiteSeo.absolute(baseUrl, path)}">
    <meta property="og:image" content="${ru.milastoria.view.SiteSeo.defaultOgImage(baseUrl)}">
    <meta property="og:image:alt" content="Milastoria — ателье нарядного платья">
    <meta name="twitter:card" content="summary_large_image">
    <meta name="twitter:title" content="${title}">
    <meta name="twitter:description" content="${description}">
    <meta name="twitter:image" content="${ru.milastoria.view.SiteSeo.defaultOgImage(baseUrl)}">

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Fraunces:ital,opsz,wght@0,9..144,300;0,9..144,400;0,9..144,500;1,9..144,400&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="/css/styles.css">

    <script type="application/ld+json">${ru.milastoria.view.RawHtml.of(ru.milastoria.view.SiteSeo.localBusinessJson(contacts, baseUrl))}</script>
    @if(extraJsonLd != null && !extraJsonLd.isBlank())
        <script type="application/ld+json">${ru.milastoria.view.RawHtml.of(extraJsonLd)}</script>
    @endif
</head>
<body>
    <header class="site-header" id="site-header">
        <div class="wrap site-header__inner">
            <a class="logo" href="/" aria-label="Milastoria — на главную">
                <img src="/assets/logo.svg" alt="Milastoria" width="140" height="24">
            </a>
            <nav class="nav" aria-label="Основная навигация">
                <a href="/" class="${activeNav.equals("home") ? "is-active" : ""}">Главная</a>
                <a href="/gallery" class="${activeNav.equals("gallery") ? "is-active" : ""}">Галерея</a>
                <a href="/#about">О нас</a>
                <a href="/#contacts">Контакты</a>
            </nav>
            @if(contacts != null && contacts.hasPhone())
                <div class="header-contacts">
                    <a href="${contacts.phoneHref()}">${contacts.phone()}</a>
                </div>
            @endif
            <button class="nav-toggle" type="button" aria-label="Меню" aria-expanded="false" data-menu-toggle>
                <span></span><span></span><span></span>
            </button>
        </div>
    </header>

    <div class="mobile-menu" id="mobile-menu">
        <nav aria-label="Мобильная навигация">
            <a href="/">Главная</a>
            <a href="/gallery">Галерея</a>
            <a href="/#about">О нас</a>
            <a href="/#contacts">Контакты</a>
        </nav>
        @if(contacts != null && contacts.hasPhone())
            <div class="header-contacts">
                <a href="${contacts.phoneHref()}">${contacts.phone()}</a>
            </div>
        @endif
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
                        @if(contacts != null && contacts.hasPhone())
                            <li><a href="${contacts.phoneHref()}">${contacts.phone()}</a></li>
                        @endif
                        @if(contacts != null && contacts.hasEmail())
                            <li><a href="mailto:${contacts.email()}">${contacts.email()}</a></li>
                        @endif
                        @if(contacts != null && contacts.hasAddress())
                            <li><span>${contacts.address()}</span></li>
                        @endif
                        @if(contacts != null && contacts.hasMessengers())
                            @for(int mi = 0; mi < contacts.messengers().size(); mi++)
                                <li>
                                    <a href="${contacts.messengers().get(mi).url()}" target="_blank" rel="noopener">${contacts.messengers().get(mi).name()}</a>
                                </li>
                            @endfor
                        @endif
                    </ul>
                </div>
            </div>
            <div class="footer-bottom">
                <span>© Milastoria</span>
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
