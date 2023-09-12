import{d as I,p as y,r as B,u as D,i as F,t as M,a7 as i,bB as g,a3 as q,o as l,c as r,e,x as $,g as t,F as A,A as E,j as f,w as m,B as N,X as S,a8 as V,$ as j}from"./index-df35a132.js";import{_ as L}from"./Breadcrumb-120bdd32.js";import{p as P}from"./parser-fdd85e1d.js";const Q={class:"page-container"},X={class:"main"},z={class:"table-responsive"},H={class:"table"},J=e("th",null,"ID",-1),K={class:"actions two"},O={class:"form-check"},R=["disabled","onChange","checked"],T={class:"actions two"},U=["onClick"],Y=["onClick"],ae=I({__name:"WireGuardView",setup(Z){const b=y(),d=B([]),{t:u}=D();function w(n){N(b,`/wireguard/${n}`)}F({handle:(n,s)=>{s?M(u(s),"error"):d.value=n.wireGuards.map(o=>({...o,...P(o.config)}))},document:i`
    query {
      wireGuards {
        ...WireGuardFragment
      }
    }
    ${g}
  `});const{mutate:v,loading:k}=q({document:i`
    mutation enableWireGuard($id: ID!, $enable: Boolean!) {
      enableWireGuard(id: $id, enable: $enable) {
        ...WireGuardFragment
      }
    }
    ${g}
  `});function G(n){v({id:n.id,enable:n.isEnabled})}function W(n){var s;S(V,{id:n.id,name:(s=n.wgInterface)==null?void 0:s.name,gql:i`
      mutation DeleteWireGuard($id: ID!) {
        deleteWireGuard(id: $id)
      }
    `,appApi:!1,typeName:"WireGuard"})}return(n,s)=>{const o=L,C=j;return l(),r("div",Q,[e("div",X,[$(o,{current:()=>n.$t("page_title.wireguard")},null,8,["current"]),e("div",z,[e("table",H,[e("thead",null,[e("tr",null,[J,e("th",null,t(n.$t("name")),1),e("th",null,t(n.$t("address")),1),e("th",null,t(n.$t("status")),1),e("th",null,t(n.$t("enabled")),1),e("th",K,t(n.$t("actions")),1)])]),e("tbody",null,[(l(!0),r(A,null,E(d.value,a=>{var c,_,p;return l(),r("tr",{key:a.id},[e("td",null,[$(C,{id:a.id,raw:a},null,8,["id","raw"])]),e("td",null,t((c=a.wgInterface)==null?void 0:c.name),1),e("td",null,t((p=(_=a.wgInterface)==null?void 0:_.address)==null?void 0:p.join(", ")),1),e("td",null,t(a.isActive?n.$t("running")+` (${f(u)("listening_port")}: ${a.listeningPort})`:n.$t("stopped")),1),e("td",null,[e("div",O,[e("md-checkbox",{"touch-target":"wrapper",disabled:f(k),onChange:h=>G(a),checked:a.isEnabled},null,40,R)])]),e("td",T,[e("a",{href:"#",class:"v-link",onClick:m(h=>w(a.id),["prevent"])},t(n.$t("edit")),9,U),e("a",{href:"#",class:"v-link",onClick:m(h=>W(a),["prevent"])},t(n.$t("delete")),9,Y)])])}),128))])])])])])}}});export{ae as default};
