#!/usr/bin/env python3
"""Writes res/values*/strings.xml for the six languages from the table below."""
import os
from xml.sax.saxutils import escape

LANGS = ["en", "fr", "de", "es", "pt", "ru"]
S = {
 "app_title": ["food log", "journal alimentaire", "esstagebuch", "diario de comidas", "diário alimentar", "дневник питания"],
 "action_ok": ["ok"] * 5 + ["ок"],
 "action_cancel": ["cancel", "annuler", "abbrechen", "cancelar", "cancelar", "отмена"],
 "settings": ["settings", "réglages", "einstellungen", "ajustes", "definições", "настройки"],
 "today": ["today", "aujourd'hui", "heute", "hoy", "hoje", "сегодня"],
 "yesterday": ["yesterday", "hier", "gestern", "ayer", "ontem", "вчера"],
 "nothing_yet": ["nothing yet", "rien pour l'instant", "noch nichts", "nada todavía", "nada por enquanto", "пока ничего"],
 "photograph": ["photograph", "photographier", "fotografieren", "fotografiar", "fotografar", "снять"],
 "activity": ["activity", "activité", "aktivität", "actividad", "atividade", "активность"],
 "activity_title": ["physical activity, now", "activité physique, maintenant", "körperliche aktivität, jetzt", "actividad física, ahora", "atividade física, agora", "физическая активность, сейчас"],
 "activity_free": ["what did you do?", "qu'avez-vous fait ?", "was haben sie gemacht?", "¿qué ha hecho?", "o que fez?", "что вы делали?"],
 "act_strength": ["strength training", "musculation", "krafttraining", "musculación", "musculação", "силовая тренировка"],
 "act_intense": ["intense activity", "activité intense", "intensive aktivität", "actividad intensa", "atividade intensa", "интенсивная нагрузка"],
 "act_endurance": ["endurance", "endurance", "ausdauer", "resistencia", "resistência", "выносливость"],
 "act_other": ["other…", "autre…", "anderes…", "otra…", "outra…", "другое…"],
 "weight": ["weight", "poids", "gewicht", "peso", "peso", "вес"],
 "weight_enter": ["enter today's weight", "saisir le poids du jour", "heutiges gewicht eingeben", "anotar el peso de hoy", "registar o peso de hoje", "ввести вес за сегодня"],
 "weight_prompt_kg": ["weight (kg)", "poids (kg)", "gewicht (kg)", "peso (kg)", "peso (kg)", "вес (кг)"],
 "weight_prompt_lb": ["weight (lb)", "poids (lb)", "gewicht (lb)", "peso (lb)", "peso (lb)", "вес (фунты)"],
 "weight_last": ["last %1$s", "dernier %1$s", "zuletzt %1$s", "último %1$s", "último %1$s", "прошлый %1$s"],
 "weight_change": ["change the weight", "modifier le poids", "gewicht ändern", "cambiar el peso", "alterar o peso", "изменить вес"],
 "weight_unit": ["weight unit", "unité de poids", "gewichtseinheit", "unidad de peso", "unidade de peso", "единица веса"],
 "curve_needs_two": ["The curve appears with the second weight.", "La courbe apparaît dès le deuxième poids.", "Die kurve erscheint ab dem zweiten gewicht.", "La curva aparece con el segundo peso.", "A curva aparece com o segundo peso.", "График появится после второго взвешивания."],
 "span_30": ["30 days", "30 jours", "30 tage", "30 días", "30 dias", "30 дней"],
 "span_90": ["90 days", "90 jours", "90 tage", "90 días", "90 dias", "90 дней"],
 "span_year": ["1 year", "1 an", "1 jahr", "1 año", "1 ano", "1 год"],
 "span_all": ["everything", "tout", "alles", "todo", "tudo", "всё"],
 "meal_breakfast": ["breakfast", "petit-déjeuner", "frühstück", "desayuno", "pequeno-almoço", "завтрак"],
 "meal_lunch": ["lunch", "déjeuner", "mittagessen", "almuerzo", "almoço", "обед"],
 "meal_dinner": ["dinner", "dîner", "abendessen", "cena", "jantar", "ужин"],
 "meal_snack": ["snack", "collation", "zwischenmahlzeit", "tentempié", "lanche", "перекус"],
 "touch_anywhere": ["touch anywhere", "touchez n'importe où", "irgendwo tippen", "toque en cualquier lugar", "toque em qualquer sítio", "коснитесь экрана"],
 "camera_needed": ["The camera is all this screen does. Touch to allow it.", "Cet écran ne sert qu'à photographier. Touchez pour autoriser l'appareil photo.", "Dieser Bildschirm fotografiert nur. Tippen, um die Kamera zu erlauben.", "Esta pantalla solo sirve para fotografiar. Toque para permitir la cámara.", "Este ecrã só serve para fotografar. Toque para permitir a câmara.", "Этот экран только фотографирует. Коснитесь, чтобы разрешить камеру."],
 "failed": ["that did not work — try again", "cela n'a pas marché — réessayez", "das hat nicht geklappt — noch einmal versuchen", "no ha funcionado — inténtelo de nuevo", "não funcionou — tente de novo", "не получилось — попробуйте ещё раз"],
 "share": ["share", "partager", "teilen", "compartir", "partilhar", "поделиться"],
 "delete": ["delete", "supprimer", "löschen", "eliminar", "eliminar", "удалить"],
 "delete_photo_q": ["delete this photo?", "supprimer cette photo ?", "dieses foto löschen?", "¿eliminar esta foto?", "eliminar esta foto?", "удалить это фото?"],
 "delete_for_good": ["delete for good", "supprimer définitivement", "endgültig löschen", "eliminar definitivamente", "eliminar definitivamente", "удалить навсегда"],
 "change_text": ["change the text", "modifier le texte", "text ändern", "cambiar el texto", "alterar o texto", "изменить текст"],
 "local_hint": ["Everything stays on this phone. To move to a new phone, export the journal here and import it there.", "Tout reste sur ce téléphone. Pour changer de téléphone, exportez le journal ici et importez-le là-bas.", "Alles bleibt auf diesem Telefon. Für ein neues Telefon das Tagebuch hier exportieren und dort importieren.", "Todo se queda en este teléfono. Para cambiar de teléfono, exporte el diario aquí e impórtelo allí.", "Tudo fica neste telemóvel. Para mudar de telemóvel, exporte o diário aqui e importe-o lá.", "Всё остаётся на этом телефоне. Для переноса на новый телефон экспортируйте дневник здесь и импортируйте там."],
 "export_journal": ["export the journal", "exporter le journal", "tagebuch exportieren", "exportar el diario", "exportar o diário", "экспорт дневника"],
 "export_hint": ["one zip file, where you choose", "un fichier zip, où vous voulez", "eine zip-datei, wo sie wollen", "un archivo zip, donde quiera", "um ficheiro zip, onde quiser", "один zip-файл, куда хотите"],
 "import_journal": ["import a journal", "importer un journal", "tagebuch importieren", "importar un diario", "importar um diário", "импорт дневника"],
 "import_hint": ["adds, replaces nothing", "ajoute, ne remplace rien", "ergänzt, ersetzt nichts", "añade, no reemplaza nada", "junta, não substitui nada", "дополняет, не заменяет"],
 "working": ["working…", "en cours…", "läuft…", "en curso…", "a trabalhar…", "выполняется…"],
 "exported": ["journal exported", "journal exporté", "tagebuch exportiert", "diario exportado", "diário exportado", "дневник экспортирован"],
 "nothing_new": ["nothing new in this file", "rien de nouveau dans ce fichier", "nichts neues in dieser datei", "nada nuevo en este archivo", "nada de novo neste ficheiro", "в этом файле нет ничего нового"],
 "not_a_journal": ["this file is not a food log journal", "ce fichier n'est pas un journal alimentaire", "diese datei ist kein esstagebuch", "este archivo no es un diario de comidas", "este ficheiro não é um diário alimentar", "этот файл не является дневником питания"],
 "meal_hours": ["The label comes from the time of the photo: breakfast 4:00–10:30, lunch 11:30–14:30, dinner 18:00–22:00, snack the rest of the time. Touch a photo to change it.", "L'étiquette vient de l'heure de la photo : petit-déjeuner 4 h–10 h 30, déjeuner 11 h 30–14 h 30, dîner 18 h–22 h, collation le reste du temps. Touchez une photo pour la changer.", "Das etikett ergibt sich aus der uhrzeit des fotos: frühstück 4:00–10:30, mittagessen 11:30–14:30, abendessen 18:00–22:00, sonst zwischenmahlzeit. Foto antippen, um es zu ändern.", "La etiqueta viene de la hora de la foto: desayuno 4:00–10:30, almuerzo 11:30–14:30, cena 18:00–22:00, tentempié el resto del tiempo. Toque una foto para cambiarla.", "A etiqueta vem da hora da foto: pequeno-almoço 4:00–10:30, almoço 11:30–14:30, jantar 18:00–22:00, lanche no resto do tempo. Toque numa foto para a alterar.", "Метка зависит от времени снимка: завтрак 4:00–10:30, обед 11:30–14:30, ужин 18:00–22:00, в остальное время — перекус. Коснитесь фото, чтобы изменить её."],
 "theme_dark": ["dark", "sombre", "dunkel", "oscuro", "escuro", "тёмная"],
 "theme_light": ["light", "clair", "hell", "claro", "claro", "светлая"],
 "colours": ["colours", "couleurs", "farben", "colores", "cores", "цвета"],
 "text_size": ["text size", "taille du texte", "textgrösse", "tamaño del texto", "tamanho do texto", "размер текста"],
 "font": ["font", "police", "schrift", "fuente", "tipo de letra", "шрифт"],
 "haptics": ["vibration on touch", "vibration au toucher", "vibration beim tippen", "vibración al tocar", "vibração ao tocar", "вибрация при касании"],
 "on": ["on", "oui", "ein", "sí", "sim", "вкл"],
 "off": ["off", "non", "aus", "no", "não", "выкл"],
 "widget_shot_name": ["Food photo", "Photo du repas", "Essensfoto", "Foto de comida", "Foto da refeição", "Фото еды"],
 "widget_shot_desc": ["One touch opens the camera", "Un toucher ouvre l'appareil photo", "Ein tippen öffnet die kamera", "Un toque abre la cámara", "Um toque abre a câmara", "Одно касание открывает камеру"],
 "widget_line_name": ["Food log", "Journal alimentaire", "Esstagebuch", "Diario de comidas", "Diário alimentar", "Дневник питания"],
 "widget_line_desc": ["Today's count, with the camera and the activity pencil", "Le décompte du jour, avec l'appareil photo et le crayon d'activité", "Der heutige stand, mit kamera und aktivitätsstift", "El recuento de hoy, con la cámara y el lápiz de actividad", "A contagem de hoje, com a câmara e o lápis de atividade", "Итог дня, камера и карандаш активности"],
 "tile_label": ["Food photo", "Photo du repas", "Essensfoto", "Foto de comida", "Foto da refeição", "Фото еды"],
 "shortcut_photo": ["Photograph", "Photographier", "Fotografieren", "Fotografiar", "Fotografar", "Снять"],
 "shortcut_activity": ["Activity", "Activité", "Aktivität", "Actividad", "Atividade", "Активность"],
}
# quantity -> text, per language
P = {
 "n_photos": [{"one": "%d photo", "other": "%d photos"}, {"one": "%d photo", "other": "%d photos"}, {"one": "%d foto", "other": "%d fotos"}, {"one": "%d foto", "other": "%d fotos"}, {"one": "%d foto", "other": "%d fotos"}, {"one": "%d фото", "few": "%d фото", "many": "%d фото", "other": "%d фото"}],
 "n_activities": [{"one": "%d activity", "other": "%d activities"}, {"one": "%d activité", "other": "%d activités"}, {"one": "%d aktivität", "other": "%d aktivitäten"}, {"one": "%d actividad", "other": "%d actividades"}, {"one": "%d atividade", "other": "%d atividades"}, {"one": "%d активность", "few": "%d активности", "many": "%d активностей", "other": "%d активности"}],
 "imported": [{"one": "%d entry added", "other": "%d entries added"}, {"one": "%d entrée ajoutée", "other": "%d entrées ajoutées"}, {"one": "%d eintrag hinzugefügt", "other": "%d einträge hinzugefügt"}, {"one": "%d entrada añadida", "other": "%d entradas añadidas"}, {"one": "%d entrada adicionada", "other": "%d entradas adicionadas"}, {"one": "добавлена %d запись", "few": "добавлено %d записи", "many": "добавлено %d записей", "other": "добавлено %d записи"}],
}

def esc(t): return escape(t).replace("'", "\\'")

res = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")
for i, lang in enumerate(LANGS):
    d = os.path.join(res, "values" if lang == "en" else f"values-{lang}")
    os.makedirs(d, exist_ok=True)
    out = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    if lang == "en": out.append('    <string name="app_name">Reader\\\'s Food Log</string>')
    for k, v in S.items():
        assert len(v) == 6, k
        out.append(f'    <string name="{k}">{esc(v[i])}</string>')
    for k, v in P.items():
        out.append(f'    <plurals name="{k}">')
        out += [f'        <item quantity="{q}">{esc(t)}</item>' for q, t in v[i].items()]
        out.append("    </plurals>")
    out.append("</resources>")
    open(os.path.join(d, "strings.xml"), "w").write("\n".join(out) + "\n")
