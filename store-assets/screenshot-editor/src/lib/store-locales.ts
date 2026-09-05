export const STORE_LOCALES = [
  "ar", "az", "be", "bn", "da", "de", "el", "en", "es", "fi",
  "fr", "he", "hi", "hu", "hy", "id", "it", "ja", "ka", "kk",
  "ko", "ky", "nb", "nl", "pl", "pt", "ro", "ru", "sv", "th",
  "tr", "uk", "vi", "zh",
] as const;

export type StoreLocale = (typeof STORE_LOCALES)[number];

export const RTL_LOCALES = new Set<StoreLocale>(["ar", "he"]);

export const APP_STORE_LOCALES: Readonly<Record<StoreLocale, string | null>> = {
  ar: "ar-SA", az: null, be: null, bn: "bn-BD", da: "da", de: "de-DE",
  el: "el", en: "en-US", es: "es-ES", fi: "fi", fr: "fr-FR", he: "he",
  hi: "hi", hu: "hu", hy: null, id: "id", it: "it", ja: "ja", ka: null,
  kk: null, ko: "ko", ky: null, nb: "no", nl: "nl-NL", pl: "pl",
  pt: "pt-BR", ro: "ro", ru: "ru", sv: "sv", th: "th", tr: "tr",
  uk: "uk", vi: "vi", zh: "zh-Hant",
};

export const PLAY_STORE_LOCALES: Readonly<Record<StoreLocale, string>> = {
  ar: "ar", az: "az-AZ", be: "be", bn: "bn-BD", da: "da-DK",
  de: "de-DE", el: "el-GR", en: "en-US", es: "es-ES", fi: "fi-FI",
  fr: "fr-FR", he: "iw-IL", hi: "hi-IN", hu: "hu-HU", hy: "hy-AM",
  id: "id", it: "it-IT", ja: "ja-JP", ka: "ka-GE", kk: "kk",
  ko: "ko-KR", ky: "ky-KG", nb: "no-NO", nl: "nl-NL", pl: "pl-PL",
  pt: "pt-BR", ro: "ro", ru: "ru-RU", sv: "sv-SE", th: "th",
  tr: "tr-TR", uk: "uk", vi: "vi", zh: "zh-TW",
};

export function directionForLocale(locale: string): "ltr" | "rtl" {
  return RTL_LOCALES.has(locale as StoreLocale) ? "rtl" : "ltr";
}

export function isStoreLocale(locale: string): locale is StoreLocale {
  return (STORE_LOCALES as readonly string[]).includes(locale);
}
