
# Format of column changed, no information was stored here earlier though
alter table UserData drop column extendedInformationData;
alter table UserData add extendedInformationData TEXT;
