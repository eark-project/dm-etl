
function callback(data) {
  var lilyEndpoint = 'http://172.20.30.61:12060/repository/record/';
  var searchResults = '';
  for (doc of data.response.docs) {
    var fileAdress = lilyEndpoint + encodeURIComponent(doc['lily.id']) +
        '/field/n$body/data?ns.n=at.ac.ait';
    var link = '<a href="' + fileAdress + '">' + doc.url + '</a>';
    searchResults += '<li>' + link + '</li>';
  }
  document.getElementById('output').innerHTML = '<ul>' + searchResults + '</ul>';
}

function askSolr() {
  var solrEndpoint = 'http://172.20.30.219:8983/solr/collection1/';
  var value = document.forms.find.queryString.value;
  if (!value) value = '*';
  var script = document.createElement('script');
  script.src = solrEndpoint + 'select?q=body%3A' + value + '&wt=json&json.wrf=callback';
  document.getElementsByTagName('head')[0].appendChild(script);
}
