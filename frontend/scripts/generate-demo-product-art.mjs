import { mkdir, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..', 'public', 'assets', 'products')

const illustrations = {
  'technova-smartphone.svg': `
    <rect x="181" y="82" width="150" height="310" rx="30" fill="#182c38"/>
    <rect x="190" y="96" width="132" height="280" rx="22" fill="#dcece6"/>
    <path d="M194 298c41-71 84-96 124-117v191H194z" fill="#78b9aa"/>
    <path d="M194 331c36-42 77-61 128-69v110H194z" fill="#477b72"/>
    <rect x="232" y="105" width="48" height="7" rx="4" fill="#263f4a"/>
    <circle cx="256" cy="363" r="5" fill="#8fa9a3"/>`,
  'technova-laptop.svg': `
    <rect x="135" y="98" width="242" height="190" rx="17" fill="#263945"/>
    <rect x="147" y="110" width="218" height="164" rx="9" fill="#d8ece8"/>
    <path d="M150 246c57-76 124-81 212-117v145H150z" fill="#81bcb0"/>
    <path d="M111 295h290l29 35q3 10-12 12H93q-15-2-12-12z" fill="#aebdb8"/>
    <path d="M197 300h117l-10 12h-98z" fill="#81968f"/>
    <path d="M145 298h214" stroke="#dce5e1" stroke-width="3"/>`,
  'technova-headphones.svg': `
    <path d="M145 270v-45c0-75 49-126 111-126s111 51 111 126v45" fill="none" stroke="#263c48" stroke-width="30" stroke-linecap="round"/>
    <path d="M152 218v-12c0-63 45-105 104-105s104 42 104 105v12" fill="none" stroke="#66877f" stroke-width="10"/>
    <rect x="123" y="218" width="62" height="125" rx="25" fill="#263c48"/>
    <rect x="327" y="218" width="62" height="125" rx="25" fill="#263c48"/>
    <rect x="135" y="231" width="38" height="98" rx="18" fill="#93bdb1"/>
    <rect x="339" y="231" width="38" height="98" rx="18" fill="#93bdb1"/>`,
  'technova-keyboard.svg': `
    <rect x="92" y="160" width="328" height="204" rx="24" fill="#26343e"/>
    <g fill="#d5e5df" stroke="#91aaa2" stroke-width="2">
      <rect x="112" y="181" width="35" height="30" rx="6"/><rect x="155" y="181" width="35" height="30" rx="6"/><rect x="198" y="181" width="35" height="30" rx="6"/><rect x="241" y="181" width="35" height="30" rx="6"/><rect x="284" y="181" width="35" height="30" rx="6"/><rect x="327" y="181" width="35" height="30" rx="6"/><rect x="370" y="181" width="30" height="30" rx="6"/>
      <rect x="112" y="221" width="43" height="30" rx="6"/><rect x="163" y="221" width="35" height="30" rx="6"/><rect x="206" y="221" width="35" height="30" rx="6"/><rect x="249" y="221" width="35" height="30" rx="6"/><rect x="292" y="221" width="35" height="30" rx="6"/><rect x="335" y="221" width="65" height="30" rx="6"/>
      <rect x="112" y="261" width="52" height="30" rx="6"/><rect x="172" y="261" width="35" height="30" rx="6"/><rect x="215" y="261" width="35" height="30" rx="6"/><rect x="258" y="261" width="35" height="30" rx="6"/><rect x="301" y="261" width="35" height="30" rx="6"/><rect x="344" y="261" width="56" height="30" rx="6"/>
      <rect x="112" y="301" width="56" height="30" rx="6"/><rect x="176" y="301" width="170" height="30" rx="6"/><rect x="354" y="301" width="46" height="30" rx="6"/>
    </g>
    <path d="M120 350h272" stroke="#c58e78" stroke-width="5" stroke-linecap="round"/>`,
  'technova-mouse.svg': `
    <path d="M256 91c-69 0-112 53-112 132v89c0 69 46 112 112 112s112-43 112-112v-89c0-79-43-132-112-132z" fill="#293d48"/>
    <path d="M256 95v102" stroke="#8ca69f" stroke-width="6"/>
    <path d="M149 220h99v35h-99" fill="#405963"/>
    <path d="M263 220h100v35H263" fill="#405963"/>
    <rect x="246" y="212" width="20" height="48" rx="10" fill="#b7d9ce"/>
    <path d="M176 319q80 34 160 0" fill="none" stroke="#70a99b" stroke-width="8" stroke-linecap="round"/>
    <circle cx="256" cy="354" r="7" fill="#c58e78"/>`,
  'technova-usb-hub.svg': `
    <path d="M177 249h-71c-26 0-40 16-40 40v16" fill="none" stroke="#556e78" stroke-width="12" stroke-linecap="round"/>
    <rect x="126" y="177" width="260" height="148" rx="35" fill="#9eafa9"/>
    <rect x="141" y="191" width="230" height="120" rx="25" fill="#dce5e1"/>
    <rect x="160" y="221" width="50" height="35" rx="5" fill="#344b55"/><rect x="222" y="221" width="50" height="35" rx="5" fill="#344b55"/>
    <circle cx="306" cy="238" r="17" fill="#344b55"/><circle cx="350" cy="238" r="12" fill="#668279"/>
    <path d="M170 281h174" stroke="#aec4bb" stroke-width="7" stroke-linecap="round"/>`,
  'technova-fitness-band.svg': `
    <path d="M210 83q46-27 92 0l18 85h-38l-8-40h-44l-8 40h-38zM190 344h42l-9 45q33 22 66 0l-9-45h42l18 81q-57 50-114 0z" fill="#506c68"/>
    <rect x="184" y="150" width="144" height="214" rx="47" fill="#283d47"/>
    <rect x="201" y="169" width="110" height="176" rx="34" fill="#b7d8ce"/>
    <path d="M220 265a36 36 0 0 1 72 0" fill="none" stroke="#5a9d8d" stroke-width="10" stroke-linecap="round"/>
    <path d="M236 276l16 15 27-36" fill="none" stroke="#344e56" stroke-width="8" stroke-linecap="round" stroke-linejoin="round"/>`,
  'technova-monitor.svg': `
    <rect x="96" y="91" width="320" height="218" rx="18" fill="#253b46"/>
    <rect x="110" y="105" width="292" height="190" rx="9" fill="#d9ece7"/>
    <path d="M114 267q80-104 146-54t138-78v156H114z" fill="#73ad9f"/>
    <path d="M114 278q70-67 137-26t147-44v87H114z" fill="#456f6a"/>
    <path d="M230 309h52v48h-52z" fill="#839b94"/><path d="M183 358h147q17 0 22 15H161q5-15 22-15z" fill="#aebdb8"/>
    <circle cx="256" cy="300" r="3" fill="#c9d8d3"/>`,
  'urbanstyle-tshirt.svg': `
    <path d="M204 115l35-20h34l35 20 64 41-35 70-37-23v154H178V203l-37 23-35-70z" fill="#47766a"/>
    <path d="M239 95q17 37 34 0" fill="none" stroke="#dcebe5" stroke-width="9"/>
    <path d="M178 333h122" stroke="#365f57" stroke-width="5" opacity=".55"/>
    <path d="M132 164l30 20m218-20-30 20" stroke="#91b6a9" stroke-width="7" stroke-linecap="round"/>`,
  'urbanstyle-jeans.svg': `
    <path d="M174 96h164l30 286H269l-13-137-13 137H144z" fill="#527c9a"/>
    <path d="M174 96h164l8 61H167z" fill="#395f7b"/>
    <path d="M256 100v145m-78-88h156" stroke="#a9c2ce" stroke-width="5"/>
    <path d="M188 178l17 8-15 26m134-34-17 8 15 26" fill="none" stroke="#b6cbd4" stroke-width="4"/>
    <path d="M148 365h112m9 0h90" stroke="#334f68" stroke-width="8" stroke-linecap="round"/>
    <circle cx="256" cy="128" r="5" fill="#d7bd84"/>`,
  'urbanstyle-running-shoes.svg': `
    <path d="M119 279q54-8 79-86l39 18q22 65 91 79l53 12q28 8 35 39l-2 30H118q-17-39 1-92z" fill="#f4f2eb"/>
    <path d="M198 193l39 18q22 65 91 79l-34 46H139q43-33 59-143z" fill="#6b9b8b"/>
    <path d="M118 340h293q12 0 9 17l-2 12H121q-15-8-3-29z" fill="#405d61"/>
    <path d="M216 229l48 34m-60-18 49 34m-61-17 47 32" stroke="#f8f6ef" stroke-width="7" stroke-linecap="round"/>
    <path d="M180 309h55" stroke="#d5e5df" stroke-width="7" stroke-linecap="round"/>`,
  'urbanstyle-hoodie.svg': `
    <path d="M201 149q0-57 55-57t55 57l47 17 52 70-39 33-36-32v139H177V237l-36 32-39-33 52-70z" fill="#627b83"/>
    <path d="M207 144q4-37 49-37t49 37q-12 38-49 43-37-5-49-43z" fill="#425a64"/>
    <path d="M256 187v183m-13-177-9 58m35-58 9 58" stroke="#c6d8d4" stroke-width="5"/>
    <path d="M205 286q51-24 102 0v48H205z" fill="#506a72" stroke="#91a7a8" stroke-width="4"/>
    <path d="M178 367h156" stroke="#405961" stroke-width="8"/>`,
  'urbanstyle-backpack.svg': `
    <path d="M190 171v-23q0-66 66-66t66 66v23" fill="none" stroke="#9d6849" stroke-width="19"/>
    <path d="M164 160q0-26 26-26h132q26 0 26 26v204q0 29-28 29H192q-28 0-28-29z" fill="#9a674b"/>
    <path d="M182 256q74-36 148 0v94q0 18-19 18H201q-19 0-19-18z" fill="#b17c5e" stroke="#754f3c" stroke-width="5"/>
    <path d="M207 261v38q49 20 98 0v-38" fill="none" stroke="#d8b49c" stroke-width="5"/>
    <path d="M151 187q-16 66 0 127m210-127q16 66 0 127" fill="none" stroke="#654c40" stroke-width="12" stroke-linecap="round"/>
    <rect x="237" y="316" width="22" height="15" rx="4" fill="#e0c295"/>`,
  'urbanstyle-watch.svg': `
    <path d="M222 79h68l21 104h-42l-9-45-9 45h-42zM218 328h42l9 45 9-45h42l-21 104h-68z" fill="#704e3f"/>
    <rect x="180" y="158" width="152" height="193" rx="48" fill="#405b59"/>
    <circle cx="256" cy="253" r="62" fill="#f1e8d5" stroke="#c4a779" stroke-width="9"/>
    <circle cx="256" cy="253" r="5" fill="#33494b"/>
    <path d="M256 253v-39m0 39 30 17" stroke="#33494b" stroke-width="6" stroke-linecap="round"/>
    <path d="M256 201v8m0 88v8m-52-52h8m88 0h8" stroke="#687d72" stroke-width="4"/>`,
  'urbanstyle-bomber-jacket.svg': `
    <path d="M206 124l36-22h28l36 22 58 36 47 62-43 39-38-34v144H182V227l-38 34-43-39 47-62z" fill="#49665f"/>
    <path d="M242 102q14 34 28 0l28 22-30 45h-24l-30-45z" fill="#d6e2da"/>
    <path d="M256 169v194" stroke="#d5c49d" stroke-width="5"/>
    <path d="M183 350h146m-140 13h134" stroke="#bfd0c8" stroke-width="5"/>
    <path d="M181 228q-13 68 0 107m150-107q13 68 0 107" fill="none" stroke="#304d48" stroke-width="7"/>
    <path d="M189 261h43v47h-43m104-47h43v47h-43" fill="#3c5a54" stroke="#78928a" stroke-width="4"/>`,
  'urbanstyle-shirt.svg': `
    <path d="M207 120l35-22h28l35 22 57 34-32 65-40-22v163H222V197l-40 22-32-65z" fill="#d5e2d9" stroke="#91aaa0" stroke-width="5"/>
    <path d="M242 98l14 34 14-34 31 22-31 52h-28l-31-52z" fill="#f2f3ec" stroke="#91aaa0" stroke-width="4"/>
    <path d="M256 150v188" stroke="#8ca49a" stroke-width="4"/>
    <circle cx="256" cy="184" r="3" fill="#6f8c81"/><circle cx="256" cy="224" r="3" fill="#6f8c81"/><circle cx="256" cy="264" r="3" fill="#6f8c81"/><circle cx="256" cy="304" r="3" fill="#6f8c81"/>
    <path d="M300 220h21v37h-21z" fill="#b7cbc0"/>`,
  'homenest-dinnerware.svg': `
    <ellipse cx="202" cy="282" rx="112" ry="78" fill="#dce7de" stroke="#8fa69b" stroke-width="7"/>
    <ellipse cx="202" cy="282" rx="83" ry="54" fill="#f8f5ec" stroke="#b9c8bb" stroke-width="5"/>
    <ellipse cx="310" cy="260" rx="72" ry="58" fill="#f8f5ec" stroke="#aabdb0" stroke-width="7"/>
    <ellipse cx="310" cy="260" rx="48" ry="37" fill="#e4eee7"/>
    <path d="M306 171h44q17 0 17 19v45h-61z" fill="#dce7de" stroke="#8fa69b" stroke-width="6"/>
    <path d="M367 186q43-2 35 34-5 21-35 18" fill="none" stroke="#8fa69b" stroke-width="8"/>
    <path d="M128 282h148m-35-22q-39 38 0 76" fill="none" stroke="#b5c7b9" stroke-width="4"/>`,
  'homenest-table-lamp.svg': `
    <path d="M175 116h162l-25 104H200z" fill="#d8bd8c" stroke="#a6895f" stroke-width="6"/>
    <path d="M256 221v110" stroke="#657c70" stroke-width="14"/>
    <path d="M230 325h52l33 38q-59 26-118 0z" fill="#9a7956"/>
    <ellipse cx="256" cy="371" rx="103" ry="18" fill="#6f5c48"/>
    <path d="M197 211h118" stroke="#eddfbd" stroke-width="7"/>
    <path d="M213 131h86" stroke="#ead8ad" stroke-width="5"/>`,
  'homenest-cookware.svg': `
    <path d="M146 205q0-20 22-20h177q22 0 22 20v117q0 29-29 29H175q-29 0-29-29z" fill="#738c88"/>
    <path d="M161 213h191v97q0 21-21 21H182q-21 0-21-21z" fill="#c9d7d1"/>
    <path d="M133 216h-34v52h34m242-52h34v52h-34" fill="none" stroke="#506d6a" stroke-width="13" stroke-linejoin="round"/>
    <ellipse cx="256" cy="191" rx="112" ry="20" fill="#526c6a"/>
    <ellipse cx="256" cy="185" rx="98" ry="16" fill="#e6ece6" stroke="#9aada6" stroke-width="5"/>
    <path d="M242 164q2-26 14-26t14 26" fill="none" stroke="#647a73" stroke-width="9" stroke-linecap="round"/>
    <path d="M178 360h156" stroke="#b2c5be" stroke-width="7" stroke-linecap="round"/>`,
  'homenest-storage-organizer.svg': `
    <rect x="137" y="103" width="238" height="281" rx="18" fill="#a97652"/>
    <rect x="153" y="119" width="206" height="249" rx="9" fill="#d6b18b"/>
    <path d="M153 200h206m-206 83h206" stroke="#8b6046" stroke-width="12"/>
    <rect x="169" y="139" width="77" height="47" rx="8" fill="#839b89"/><rect x="264" y="139" width="78" height="47" rx="8" fill="#e9dfc8"/>
    <rect x="169" y="219" width="78" height="47" rx="8" fill="#e9dfc8"/><rect x="264" y="219" width="78" height="47" rx="8" fill="#839b89"/>
    <rect x="169" y="302" width="173" height="49" rx="8" fill="#c49670"/>
    <path d="M201 164h14m82 0h14m-82 79h14m82 0h14m-59 83h14" stroke="#6d5342" stroke-width="6" stroke-linecap="round"/>`,
  'homenest-kettle.svg': `
    <path d="M170 193q0-28 29-28h105q29 0 29 28v148q0 31-31 31h-99q-33 0-33-31z" fill="#c4d0ca" stroke="#718780" stroke-width="8"/>
    <path d="M333 211l65 40-65 44" fill="#aebeb7" stroke="#718780" stroke-width="8" stroke-linejoin="round"/>
    <path d="M174 214q-51 3-45 72t47 65" fill="none" stroke="#647d77" stroke-width="16"/>
    <path d="M202 165q3-47 50-47t50 47" fill="none" stroke="#657c76" stroke-width="11"/>
    <path d="M238 119q3-21 14-21t14 21" fill="none" stroke="#657c76" stroke-width="8"/>
    <rect x="180" y="371" width="160" height="20" rx="10" fill="#526964"/>
    <path d="M206 260h91" stroke="#eef1eb" stroke-width="8" stroke-linecap="round"/>`,
  'homenest-wall-clock.svg': `
    <circle cx="256" cy="247" r="143" fill="#9b7657"/>
    <circle cx="256" cy="247" r="126" fill="#f2ead9" stroke="#d3bd96" stroke-width="8"/>
    <circle cx="256" cy="247" r="8" fill="#405d56"/>
    <path d="M256 247V158m0 89 66 42" fill="none" stroke="#405d56" stroke-width="11" stroke-linecap="round"/>
    <path d="M256 136v15m0 192v15m-111-111h15m192 0h15m-190-79 11 11m136 136 11 11m0-158-11 11m-136 136-11 11" stroke="#8a765c" stroke-width="7" stroke-linecap="round"/>
    <path d="M232 98q24-30 48 0" fill="none" stroke="#6f5947" stroke-width="8"/>`,
  'homenest-cushions.svg': `
    <rect x="101" y="174" width="177" height="168" rx="35" transform="rotate(-8 101 174)" fill="#bd8772" stroke="#986957" stroke-width="7"/>
    <rect x="231" y="130" width="174" height="174" rx="38" transform="rotate(8 231 130)" fill="#7c9b8f" stroke="#5f7b70" stroke-width="7"/>
    <rect x="197" y="235" width="163" height="153" rx="34" transform="rotate(-3 197 235)" fill="#d4b886" stroke="#ad9365" stroke-width="7"/>
    <path d="M146 255q54 35 111-8m34-65q31 42 80 48m-136 77q50 35 109 2" fill="none" stroke="#f0ded0" stroke-width="6" stroke-linecap="round" opacity=".8"/>
    <circle cx="190" cy="271" r="6" fill="#f2dfd3"/><circle cx="323" cy="197" r="6" fill="#dce9df"/><circle cx="280" cy="326" r="6" fill="#f2e7d2"/>`,
  'homenest-spice-rack.svg': `
    <path d="M114 165h284m-284 95h284m-284 95h284" stroke="#8f6448" stroke-width="17" stroke-linecap="round"/>
    <path d="M139 166v189m234-189v189" stroke="#ad805c" stroke-width="13"/>
    <g stroke="#8a7660" stroke-width="5">
      <path d="M151 115h45v50h-45z" fill="#d59d55"/><path d="M204 103h45v62h-45z" fill="#8a5543"/><path d="M258 120h45v45h-45z" fill="#c78b4e"/><path d="M311 108h45v57h-45z" fill="#8b9b69"/>
      <path d="M151 210h45v50h-45z" fill="#9e6749"/><path d="M204 199h45v61h-45z" fill="#d3a54f"/><path d="M258 216h45v44h-45z" fill="#7e9163"/><path d="M311 205h45v55h-45z" fill="#b76f4c"/>
      <path d="M151 305h45v50h-45z" fill="#be814d"/><path d="M204 294h45v61h-45z" fill="#82945e"/><path d="M258 311h45v44h-45z" fill="#a55e45"/><path d="M311 300h45v55h-45z" fill="#d0a650"/>
    </g>
    <g fill="#dfd5bf"><rect x="163" y="116" width="21" height="8" rx="4"/><rect x="216" y="104" width="21" height="8" rx="4"/><rect x="270" y="121" width="21" height="8" rx="4"/><rect x="323" y="109" width="21" height="8" rx="4"/></g>`
}

const frame = (art) => `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">
  <defs>
    <linearGradient id="background" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#f7f5ef"/><stop offset="1" stop-color="#e5f0e9"/></linearGradient>
    <filter id="shadow" x="-25%" y="-25%" width="150%" height="160%"><feDropShadow dx="0" dy="13" stdDeviation="12" flood-color="#253b35" flood-opacity=".17"/></filter>
  </defs>
  <rect width="512" height="512" rx="30" fill="url(#background)"/>
  <circle cx="256" cy="245" r="183" fill="#fff" opacity=".48"/>
  <g filter="url(#shadow)" stroke-linejoin="round" stroke-linecap="round">${art}</g>
</svg>
`

await mkdir(root, { recursive: true })
for (const [filename, art] of Object.entries(illustrations)) {
  await writeFile(resolve(root, filename), frame(art), 'utf8')
}
console.log(`Generated ${Object.keys(illustrations).length} original demo product illustrations in ${root}`)
