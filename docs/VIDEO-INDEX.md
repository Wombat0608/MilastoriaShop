# Milastoria — индекс видеофайлов

Все видео рабочей папки `/mnt/storage/Projects/MilaStoria_Tilda` — абсолютные пути, размер, дата изменения.
Файлы не читаются и не меняются: индекс собран из метаданных файловой системы.

**Сгенерирован:** 2026-11-03 · **Файлов:** 76 · **Общий размер:** 2088.0 MB

---

## Как обновить

Индекс устаревает, когда в проект добавляют новое видео. Перегенерация:

```bash
cd /mnt/storage/Projects/MilaStoria_Tilda
find "$PWD" -type f \
  \( -iname '*.mp4' -o -iname '*.mov' -o -iname '*.avi' -o -iname '*.mkv' \
     -o -iname '*.webm' -o -iname '*.m4v' -o -iname '*.3gp' -o -iname '*.mpg' \
     -o -iname '*.mpeg' -o -iname '*.wmv' -o -iname '*.flv' -o -iname '*.m2ts' \
     -o -iname '*.mts' -o -iname '*.hevc' -o -iname '*.ogv' \) \
  -printf '%p\t%s\t%TY-%Tm-%Td %TH:%TM\n' | sort
```

---

## Сводка по папкам

| Папка | Файлов | Размер (MB) |
|---|---:|---:|
| `Сырье/` | 51 | 1923.0 |
| `pictures/` | 17 | 150.2 |
| `prototype/` | 8 | 14.8 |
| **ИТОГО** | **76** | **2088.0** |

Размеры в таблицах округлены до 0.1 MB, поэтому сумма строк может отличаться на 0.1.

---

## Сырье/

51 файлов · 1923.0 MB

| Абсолютный путь | Размер (MB) | Изменён |
|---|---:|---|
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/Золотой бант.mp4` | 205.6 | 2024-11-23 19:16 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/нг/2024-12-03 11-06-25.MOV` | 144.2 | 2024-12-10 09:46 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/16.mp4` | 120.7 | 2024-11-22 16:19 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/34.mp4` | 117.5 | 2024-11-22 16:20 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/28.mp4` | 98.8 | 2024-11-22 18:25 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/17.mp4` | 81.0 | 2024-11-30 08:15 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/rozovoe_paetki_IMG_7415.mov` | 71.1 | 2024-12-08 17:30 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/IMG_7398.mov` | 55.8 | 2024-11-21 13:14 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/copy_857AF46F-1834-49B7-BD83-D3C49ABBA216.mov` | 54.0 | 2024-11-21 10:44 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/IMG_7636.MOV` | 53.8 | 2024-11-22 18:20 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/нг/2024-12-03 11-07-01.MOV` | 52.3 | 2024-12-10 09:49 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/NewYear.mp4` | 51.0 | 2024-12-10 12:58 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/13.mp4` | 50.0 | 2024-11-22 16:19 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/23.mp4` | 49.7 | 2024-11-30 08:02 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/copy_ED94779A-AB8A-4896-A9DC-53EA3DFF3C06.mov` | 46.3 | 2024-11-26 14:41 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/familylook/IMG_7462.mov` | 44.0 | 2024-12-02 18:28 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/IMG_7391.mov` | 41.0 | 2024-12-05 11:28 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/детское/IMG_7391.mov` | 41.0 | 2024-11-30 08:21 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/IMG_7795.MOV` | 38.4 | 2024-11-22 18:19 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/IMG_7489.mov` | 37.8 | 2024-11-23 12:11 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/zoloto_na_chernom.mov` | 37.6 | 2024-11-26 10:44 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/нг/2024-12-03 11-13-49.MOV` | 36.0 | 2024-12-10 09:49 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/IMG_7342.mov` | 32.7 | 2024-12-05 18:34 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Полина191224/video-output-C107E088-E713-44D9-9975-96D3C8304FB4.MOV` | 32.3 | 2024-12-19 10:15 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/IMG_7495.mov` | 31.8 | 2024-12-02 09:48 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/IMG_7334.mov` | 30.9 | 2024-11-22 18:26 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/детское/IMG_7426.mov` | 27.7 | 2024-11-30 08:22 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/stairs1.mp4` | 22.3 | 2024-11-22 18:00 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/golden_dress_01.mp4` | 21.2 | 2024-11-22 19:02 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/детское/IMG_7423.mov` | 18.4 | 2024-11-30 08:22 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/output.mp4` | 17.6 | 2024-11-23 22:16 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/Output.mp4` | 17.5 | 2024-11-23 23:09 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/familylook/IMG_7509.mov` | 17.3 | 2024-12-02 18:16 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Барби/barbie_video01.mp4` | 15.5 | 2024-12-14 13:38 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/нг/2024-12-03 11-14-02.MOV` | 13.5 | 2024-12-10 09:47 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Бутон/buton_video_01.mp4` | 12.8 | 2024-12-16 13:13 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/familylook/IMG_7508.mov` | 11.3 | 2024-12-02 18:15 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/zolotoj_bant_02.mp4` | 8.3 | 2024-11-26 14:45 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/Видео для зотолотого банта.mp4` | 7.3 | 2024-11-23 21:01 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/zolotye_rozy_chernom.mp4` | 7.2 | 2024-11-26 10:56 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Барби/IMG_6198.MP4` | 6.9 | 2024-12-14 12:38 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Бутон/IMG_4837.MOV` | 6.8 | 2024-12-16 12:19 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Пышное19122024/IMG_4837.MOV` | 6.8 | 2024-12-19 10:25 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/Видео для зотолотого банта.mov` | 6.6 | 2024-11-23 20:34 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/zolotoj_bant.mp4` | 5.8 | 2024-11-26 11:02 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Бутон/buton_video_02.mp4` | 4.6 | 2024-12-16 13:15 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Барби/IMG_7359.MOV` | 4.2 | 2024-12-13 18:54 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Барби/IMG_6199.MP4` | 3.4 | 2024-12-14 12:38 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Барби/IMG_6118.MP4` | 3.2 | 2024-12-14 12:38 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Барби/IMG_6117.MP4` | 0.8 | 2024-12-14 12:38 |
| `/mnt/storage/Projects/MilaStoria_Tilda/Сырье/Золотые платья 191124/Видео/video1.mp4` | 0.6 | 2024-11-27 10:35 |

---

## pictures/

17 файлов · 150.2 MB

| Абсолютный путь | Размер (MB) | Изменён |
|---|---:|---|
| `/mnt/storage/Projects/MilaStoria_Tilda/pictures/dresses/София_01/video-output-C107E088-E713-44D9-9975-96D3C8304FB4.MOV` | 32.3 | 2021-12-02 13:20 |
| `/mnt/storage/Projects/MilaStoria_Tilda/pictures/dresses/DR_04/video-output-5D75E6D2-4214-43C2-A0FA-D4E79141EE2D.mov` | 17.2 | 2024-12-14 18:13 |
| `/mnt/storage/Projects/MilaStoria_Tilda/pictures/dresses/Розовая пайетка/rozovoe_pajetkah_01.mp4` | 14.0 | 2024-12-09 09:33 |
| `/mnt/storage/Projects/MilaStoria_Tilda/pictures/Карусель/Взрослые/adults_video_02.mp4` | 10.7 | 2024-12-02 10:26 |
| `/mnt/storage/Projects/MilaStoria_Tilda/pictures/Карусель/Взрослые/adults_video_01.mp4` | 9.5 | 2024-12-02 10:04 |
| `/mnt/storage/Projects/MilaStoria_Tilda/pictures/Карусель/Детские/child_video_02.mp4` | 8.6 | 2024-12-01 18:18 |
| `/mnt/storage/Projects/MilaStoria_Tilda/pictures/Карусель/Детские/child_video_01.mp4` | 8.6 | 2024-12-02 13:57 |
| `/mnt/storage/Projects/MilaStoria_Tilda/pictures/Карусель/FamilyLook/family_look_02.mp4` | 8.2 | 2024-12-03 10:15 |
| `/mnt/storage/Projects/MilaStoria_Tilda/pictures/Карусель/FamilyLook/family_look_01.mp4` | 7.3 | 2024-12-02 18:41 |
| `/mnt/storage/Projects/MilaStoria_Tilda/pictures/Карусель/Детские/ChildDress_short.mp4` | 6.8 | 2024-12-01 22:14 |
| `/mnt/storage/Projects/MilaStoria_Tilda/pictures/dresses/Трансформер/pishnoe_video_01.mp4` | 6.6 | 2024-12-21 23:03 |
| `/mnt/storage/Projects/MilaStoria_Tilda/pictures/dresses/Черное серебро/chyornoe_serebro_video_01.mp4` | 5.3 | 2024-12-05 11:57 |
| `/mnt/storage/Projects/MilaStoria_Tilda/pictures/dresses/DR_04/v_bleske_video_01.mp4` | 4.0 | 2024-12-14 18:19 |
| `/mnt/storage/Projects/MilaStoria_Tilda/pictures/dresses/DR_04/v_bleske_video01.mp4` | 3.9 | 2024-12-14 18:21 |
| `/mnt/storage/Projects/MilaStoria_Tilda/pictures/dresses/Золото_Бант/zolotoj_bant_03.mp4` | 2.9 | 2024-12-06 18:23 |
| `/mnt/storage/Projects/MilaStoria_Tilda/pictures/dresses/Полина_191224/lepestok_video01.mp4` | 2.4 | 2024-12-20 10:13 |
| `/mnt/storage/Projects/MilaStoria_Tilda/pictures/gallery/IMG_0799.MP4` | 1.9 | 2025-11-02 18:37 |

---

## prototype/

8 файлов · 14.8 MB

| Абсолютный путь | Размер (MB) | Изменён |
|---|---:|---|
| `/mnt/storage/Projects/MilaStoria_Tilda/prototype/assets/video/test-video-lot__v1.mp4` | 2.9 | 2026-10-13 16:03 |
| `/mnt/storage/Projects/MilaStoria_Tilda/prototype/assets/video/zoloto-bant.mp4` | 2.9 | 2026-09-30 16:13 |
| `/mnt/storage/Projects/MilaStoria_Tilda/prototype/assets/video/cherno-serebro.mp4` | 2.5 | 2026-09-30 16:13 |
| `/mnt/storage/Projects/MilaStoria_Tilda/prototype/assets/video/polina.mp4` | 2.4 | 2026-09-30 16:13 |
| `/mnt/storage/Projects/MilaStoria_Tilda/prototype/assets/video/child-dress.mp4` | 1.5 | 2026-09-30 16:13 |
| `/mnt/storage/Projects/MilaStoria_Tilda/prototype/assets/video/family-look.mp4` | 1.4 | 2026-09-30 16:14 |
| `/mnt/storage/Projects/MilaStoria_Tilda/prototype/assets/video/adults.mp4` | 1.2 | 2026-09-30 16:13 |
| `/mnt/storage/Projects/MilaStoria_Tilda/prototype/assets/video/gold-01__v1.mp4` | <0.1 | 2026-10-05 10:37 |

---
