devForeman=foreman start -e .env

dev:
	$(devForeman)

db:
	sh ./refresh_db.sh
	$(devForeman)