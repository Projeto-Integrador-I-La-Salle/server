#!/bin/bash
# Envia massas de produtos de farmácia para a API local (mesmo padrão de curl do seu script).

URL="${PRODUCTS_URL:-http://localhost:3000/products}"

post_product() {
	local body_json="$1"
	local response
	response=$(
		curl -s -D - -w "\nTIME_TOTAL:%{time_total}\nHTTP_STATUS:%{http_code}" \
			-X POST "$URL" \
			-H "Content-Type: application/json" \
			-d "$body_json"
	)

	local headers body time status
	headers=$(echo "$response" | sed -n '/HTTP\/1.1/,/^\r$/p')
	body=$(echo "$response" | sed -n '/^\r$/,$p' | sed '1d' | sed '/TIME_TOTAL:/,$d')
	time=$(echo "$response" | grep "TIME_TOTAL" | cut -d ':' -f2)
	status=$(echo "$response" | grep "HTTP_STATUS" | cut -d ':' -f2)

	echo "===================="
	echo "STATUS: $status"
	echo "TIME: ${time}s"
	echo "===================="

	echo -e "\nHEADERS:"
	echo "$headers"

	echo -e "\nBODY:"
	echo "$body"
	echo ""
}

# code: use faixa livre na sua base (ex.: 201+)
post_product '{
  "name": "Paracetamol 500mg — alívio de dor de cabeça (20 comprimidos)",
  "price": 8.5,
  "active": true,
  "code": 201,
  "quantity": 120,
  "brand": "EMS",
  "images": ["https://www.callfarma.com.br/_next/image?url=https%3A%2F%2Fd2lakedouw4zad.cloudfront.net%2Fparacetamol-500mg-20cpr-prati-donaduzzi-61763.png&w=640&q=75"]
}'

post_product '{
  "name": "Dipirona Sódica 500mg — dor de cabeça e febre (40 comprimidos)",
  "price": 6.9,
  "active": true,
  "code": 202,
  "quantity": 200,
  "brand": "Neo Química",
  "images": ["https://io.convertiez.com.br/m/farmaponte/shop/products/images/21049/medium/dipirona-sodica-prati-donaduzzi-500mg-caixa-com-30-comprimidos_35320.png"]
}'

post_product '{
  "name": "Ibuprofeno 400mg — dor de cabeça e inflamação (10 cápsulas)",
  "price": 14.2,
  "active": true,
  "code": 203,
  "quantity": 80,
  "brand": "Advil",
  "images": ["https://dmvfarma.vtexassets.com/arquivos/ids/323258/5fdc9e3a-9464-4bc2-b5dc-17319ba0aaf6.jpg?v=639104318573270000"]
}'

post_product '{
  "name": "Aspirina 500mg — dor de cabeça leve (20 comprimidos)",
  "price": 11,
  "active": true,
  "code": 204,
  "quantity": 60,
  "brand": "Bayer",
  "images": ["https://drogariavenancio.vtexassets.com/arquivos/ids/1105261-800-450?v=638405779245770000&width=800&height=450&aspect=true"]
}'

post_product '{
  "name": "Paracetamol + Cafeína — enxaqueca e dor tensional (16 comprimidos)",
  "price": 18.75,
  "active": true,
  "code": 205,
  "quantity": 45,
  "brand": "Dorflex",
  "images": ["https://dmvfarma.vtexassets.com/arquivos/ids/296944/edab829e-980c-42a6-9414-2c5a2276ff0f.jpg?v=638971971628670000"]
}'

post_product '{
  "name": "Óleo de menta + canfora — massagem para tensão cervical (30ml)",
  "price": 9.99,
  "active": true,
  "code": 206,
  "quantity": 35,
  "brand": "Ben-Gay",
  "images": ["https://cdn.sistemawbuy.com.br/arquivos/64413dbe23bb05ee76c46ddf73ee0ff1/produtos/6721759becacf/3656-1-672287d08cbdd.jpg"]
}'

post_product '{
  "name": "Loratadina 10mg — alergias e coriza (12 comprimidos)",
  "price": 12.4,
  "active": true,
  "code": 207,
  "quantity": 90,
  "brand": "Claritin",
  "images": ["https://www.drogaraia.com.br/_next/image?url=https%3A%2F%2Fproduct-data.raiadrogasil.io%2Fimages%2F17272306.webp&w=3840&q=40"]
}'

post_product '{
  "name": "Omeprazol 20mg — azia e refluxo (28 cápsulas)",
  "price": 22.9,
  "active": true,
  "code": 208,
  "quantity": 55,
  "brand": "Prilosec",
  "images": ["https://santaluciadrogaria.vtexassets.com/arquivos/ids/310913/7896112181729.png?v=637820192066670000"]
}'

post_product '{
  "name": "Soro fisiológico 0,9% — limpeza nasal e ferimentos (500ml)",
  "price": 4.5,
  "active": true,
  "code": 209,
  "quantity": 150,
  "brand": "Needs",
  "images": ["https://images.tcdn.com.br/img/img_prod/723410/soro_fisiologico_0_9_cloreto_de_sodio_sorimax_500ml_3613_1_20bca044b555bc34a197ac4d87c005b8.jpg"]
}'

post_product '{
  "name": "Vitamina C 1000mg — suporte imunológico (60 comprimidos)",
  "price": 19.99,
  "active": true,
  "code": 210,
  "quantity": 40,
  "brand": "Nature Made",
  "images": ["https://m.media-amazon.com/images/I/81zccW+y7EL._AC_UF1000,1000_QL80_.jpg"]
}'

