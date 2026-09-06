import fs from "node:fs";
import path from "node:path";
import process from "node:process";

const editorRoot = process.cwd();
const repositoryRoot = path.resolve(editorRoot, "../..");
const project = JSON.parse(
  fs.readFileSync(path.join(editorRoot, "app-store-screenshots.json"), "utf8"),
);

const applicationLocales = [
  "ar", "az", "be", "bn", "da", "de", "el", "en", "es", "fi",
  "fr", "he", "hi", "hu", "hy", "id", "it", "ja", "ka", "kk",
  "ko", "ky", "nb", "nl", "pl", "pt", "ro", "ru", "sv", "tg",
  "th", "tk", "tr", "uk", "uz", "vi", "zh",
];
const unsupportedByBothStores = ["tg", "tk", "uz"];
const expectedLocales = applicationLocales.filter(
  (locale) => !unsupportedByBothStores.includes(locale),
);

const resourceRoots = [
  "composeApp/src/commonMain/composeResources",
  "feature/catalog/src/commonMain/composeResources",
  "game/blockblast/src/commonMain/composeResources",
  "game/twentyfortyeight/src/commonMain/composeResources",
  "game/fruitmerge/src/commonMain/composeResources",
];

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function localesAt(relativeRoot) {
  return fs
    .readdirSync(path.join(repositoryRoot, relativeRoot), { withFileTypes: true })
    .filter((entry) => entry.isDirectory() && entry.name.startsWith("values"))
    .map((entry) => (entry.name === "values" ? "en" : entry.name.slice("values-".length)))
    .sort();
}

for (const root of resourceRoots) {
  assert(
    JSON.stringify(localesAt(root)) === JSON.stringify(applicationLocales),
    `${root} locale set differs from the application localization contract`,
  );
}

assert(
  JSON.stringify(project.locales) === JSON.stringify(expectedLocales),
  `project locales must match all ${expectedLocales.length} store-supported application locales`,
);
assert(project.locale === "en", "English must remain the editor default");

for (const device of ["iphone", "ipad", "android"]) {
  const slides = project.slidesByDevice[device];
  assert(Array.isArray(slides) && slides.length === 7, `${device} must contain seven slides`);
  for (const [index, slide] of slides.entries()) {
    for (const locale of expectedLocales) {
      assert(
        typeof slide.label?.[locale] === "string" && slide.label[locale].trim().length > 0,
        `${device} slide ${index + 1} is missing label.${locale}`,
      );
      assert(
        typeof slide.headline?.[locale] === "string" && slide.headline[locale].trim().length > 0,
        `${device} slide ${index + 1} is missing headline.${locale}`,
      );
    }
  }
}

const featureSlides = project.slidesByDevice["feature-graphic"];
assert(Array.isArray(featureSlides) && featureSlides.length === 1, "feature graphic must contain one slide");
for (const locale of expectedLocales) {
  assert(
    typeof featureSlides[0].headline?.[locale] === "string" &&
      featureSlides[0].headline[locale].trim().length > 0,
    `feature graphic is missing headline.${locale}`,
  );
}

console.log(
  `Verified complete screenshot copy for ${expectedLocales.length} store-supported locales; ` +
    `${unsupportedByBothStores.join(", ")} remain app-only.`,
);
