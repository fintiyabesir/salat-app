# Official prayer-time source strategy

The app maps **location -> country/region authority**. UI language never determines the religious authority.

## Confirmed early candidates

| Region | Authority | Source shape | Runtime plan |
|---|---|---|---|
| Malaysia | JAKIM e-Solat | Public XML daily feed observed | Device adapter when terms allow; cache locally |
| Singapore | MUIS | Annual official timetable | Download/parse yearly asset |
| Türkiye | Diyanet | Global prayer-time service + web data | Do not embed private credentials; use compliant public/download model or QA reference |
| Brunei | Ministry of Religious Affairs | Official timetable | Parse/cache published period |
| Oman | Ministry of Endowments and Religious Affairs | Official monthly timetable | Parse/cache monthly |
| Jordan | Ministry of Awqaf | Annual calendar | Prefer annual asset |
| Egypt | Egyptian General Authority of Survey | Official city/month tables | Parse/cache monthly |
| Morocco | Ministry of Habous and Islamic Affairs | Official prayer timetable | Parse/cache published period |
| Qatar | Government prayer-time service | Public endpoint observed | Direct adapter if terms permit |

The complete working research matrix is in `data/prayer_source_matrix.csv`.

## Adapter priority

1. Public official JSON/XML API/feed with suitable terms.
2. Official static JSON/XML/CSV asset.
3. Official monthly/yearly timetable asset.
4. Stable official HTML only after terms and parser stability review.
5. Local calculation only.

Every adapter must fail open: verification failure must never make prayer times unavailable.
