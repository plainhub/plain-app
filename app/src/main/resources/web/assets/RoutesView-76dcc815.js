import{d as ie,B as ce,r as E,u as de,a2 as B,al as S,c2 as I,c3 as pe,ah as _e,I as me,ai as F,o as u,e as r,f as e,j as o,k as l,M as f,c5 as A,F as b,A as k,N as x,aj as ee,x as V,y as te,m as U,ao as ve,i as fe,t as ge,a6 as L,c6 as $e,c7 as he,L as ye,P as ae,g as oe,Q as ne,w as se,V as j,a7 as be,Y as ke}from"./index-2c8e7849.js";import{_ as we}from"./Breadcrumb-01ba2071.js";import{T as m,a as w,_ as Ce,A as Ne}from"./question-mark-rounded-bae86836.js";import{u as Te,a as Ie}from"./vee-validate.esm-83a41e33.js";import"./stringToArray-36968540.js";const Fe={slot:"headline"},Ae={slot:"content"},Ee={class:"row mb-3"},Ve={class:"col-md-3 col-form-label"},De={class:"col-md-9"},Me=["value"],Re={key:0,class:"input-group mt-2"},qe=["placeholder"],Oe={class:"inner"},Se={class:"help-block"},Ue={value:""},Le=["value"],je={key:2,class:"invalid-feedback"},Be={class:"row mb-3"},Je={class:"col-md-3 col-form-label"},Pe={class:"col-md-9"},Qe=["value"],Ye={class:"row mb-3"},ze={class:"col-md-3 col-form-label"},Ge={class:"col-md-9"},He={value:"all"},Ke=["value"],We=["value"],Xe={class:"row mb-3"},Ze={class:"col-md-3 col-form-label"},xe={class:"col-md-9"},et={slot:"actions"},tt=["disabled"],le=ie({__name:"EditRouteModal",props:{data:{type:Object},devices:{type:Array},networks:{type:Array}},setup(h){var $,J,P,Q,Y,z,G,H,K;const _=h,{handleSubmit:y}=Te(),i=ce({if_name:"",apply_to:"all",notes:"",target:"",is_enabled:!0}),p=E(m.INTERNET),D=Object.values(m).filter(s=>[m.IP,m.NET,m.REMOTE_PORT,m.INTERNET].includes(s)),{t:C}=de(),{mutate:M,loading:R,onDone:q}=B({document:S`
    mutation createConfig($input: ConfigInput!) {
      createConfig(input: $input) {
        ...ConfigFragment
      }
    }
    ${I}
  `,options:{update:(s,d)=>{pe(s,d.data.createConfig,S`
          query {
            configs {
              ...ConfigFragment
            }
          }
          ${I}
        `)}}}),{mutate:O,loading:n,onDone:N}=B({document:S`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${I}
  `}),{value:c,resetField:g,errorMessage:v}=Ie("inputValue",_e().test("required",s=>"valid.required",s=>!w.hasInput(p.value)||!!s).test("target-value",s=>"invalid_value",s=>w.isValid(p.value,s??""))),a=($=_.data)==null?void 0:$.data;p.value=((P=(J=_.data)==null?void 0:J.target)==null?void 0:P.type)??m.INTERNET,c.value=((Y=(Q=_.data)==null?void 0:Q.target)==null?void 0:Y.value)??"",i.apply_to=((G=(z=_.data)==null?void 0:z.applyTo)==null?void 0:G.toValue())??"all",i.if_name=(a==null?void 0:a.if_name)??((K=(H=_.networks)==null?void 0:H[0])==null?void 0:K.ifName)??"",i.notes=(a==null?void 0:a.notes)??"",i.is_enabled=(a==null?void 0:a.is_enabled)??!0,a||g(),me(p,(s,d)=>{(s===m.INTERFACE||d===m.INTERFACE)&&(c.value="")});const T=y(()=>{const s=new w;s.type=p.value,s.value=c.value??"",i.target=s.toValue(),_.data?O({id:_.data.id,input:{group:"route",value:JSON.stringify(i)}}):M({input:{group:"route",value:JSON.stringify(i)}})});return q(()=>{F()}),N(()=>{F()}),(s,d)=>{var W,X,Z;const ue=Ce,re=ve;return u(),r("md-dialog",null,[e("div",Fe,o(l(a)?s.$t("edit"):s.$t("create")),1),e("div",Ae,[e("div",Ee,[e("label",Ve,o(s.$t("traffic_to")),1),e("div",De,[f(e("select",{class:"form-select","onUpdate:modelValue":d[0]||(d[0]=t=>p.value=t)},[(u(!0),r(b,null,k(l(D),t=>(u(),r("option",{value:t},o(s.$t(`target_type.${t}`)),9,Me))),256))],512),[[A,p.value]]),l(w).hasInput(p.value)?(u(),r("div",Re,[f(e("input",{type:"text",class:"form-control","onUpdate:modelValue":d[1]||(d[1]=t=>ee(c)?c.value=t:null),placeholder:s.$t("for_example")+" "+l(w).hint(p.value)},null,8,qe),[[x,l(c)]]),V(re,{class:"input-group-text"},{content:te(()=>[e("pre",Se,o(s.$t(`examples_${p.value}`)),1)]),default:te(()=>[e("span",Oe,[V(ue)])]),_:1})])):U("",!0),p.value===l(m).INTERFACE?f((u(),r("select",{key:1,class:"form-select mt-2","onUpdate:modelValue":d[2]||(d[2]=t=>ee(c)?c.value=t:null)},[e("option",Ue,o(s.$t("all_local_networks")),1),(u(!0),r(b,null,k((W=h.networks)==null?void 0:W.filter(t=>t.type!=="wan"),t=>(u(),r("option",{value:t.ifName},o(t.name),9,Le))),256))],512)),[[A,l(c)]]):U("",!0),l(v)?(u(),r("div",je,o(l(v)?s.$t(l(v)):""),1)):U("",!0)])]),e("div",Be,[e("label",Je,o(l(C)("route_via")),1),e("div",Pe,[f(e("select",{class:"form-select","onUpdate:modelValue":d[3]||(d[3]=t=>i.if_name=t)},[(u(!0),r(b,null,k((X=h.networks)==null?void 0:X.filter(t=>["wan","vpn"].includes(t.type)),t=>(u(),r("option",{key:t.ifName,value:t.ifName},o(t.name),9,Qe))),128))],512),[[A,i.if_name]])])]),e("div",Ye,[e("label",ze,o(l(C)("apply_to")),1),e("div",Ge,[f(e("select",{class:"form-select","onUpdate:modelValue":d[4]||(d[4]=t=>i.apply_to=t)},[e("option",He,o(s.$t("all_devices")),1),(u(!0),r(b,null,k((Z=h.networks)==null?void 0:Z.filter(t=>!["wan","vpn"].includes(t.type)),t=>(u(),r("option",{key:t.ifName,value:"iface:"+t.ifName},o(t.name),9,Ke))),128)),(u(!0),r(b,null,k(h.devices,t=>(u(),r("option",{value:"mac:"+t.mac},o(t.name),9,We))),256))],512),[[A,i.apply_to]])])]),e("div",Xe,[e("label",Ze,o(l(C)("notes")),1),e("div",xe,[f(e("textarea",{class:"form-control","onUpdate:modelValue":d[5]||(d[5]=t=>i.notes=t),rows:"3"},null,512),[[x,i.notes]])])])]),e("div",et,[e("md-outlined-button",{value:"cancel",onClick:d[6]||(d[6]=(...t)=>l(F)&&l(F)(...t))},o(s.$t("cancel")),1),e("md-filled-button",{value:"save",disabled:l(R)||l(n),onClick:d[7]||(d[7]=(...t)=>l(T)&&l(T)(...t)),autofocus:""},o(s.$t("save")),9,tt)])])}}}),at={class:"page-container"},ot={class:"main"},nt={class:"v-toolbar"},st={class:"table-responsive"},lt={class:"table"},it=e("th",null,"ID",-1),dt={class:"actions two"},ut={class:"form-check"},rt=["disabled","onChange","checked"],ct={class:"nowrap"},pt={class:"nowrap"},_t={class:"actions two"},mt=["onClick"],vt=["onClick"],bt=ie({__name:"RoutesView",setup(h){const _=E([]),y=E([]),i=E([]),{t:p}=de();fe({handle:(n,N)=>{N?ge(p(N),"error"):(_.value=n.configs.filter(c=>c.group==="route").map(c=>{const g=JSON.parse(c.value),v=new Ne;v.parse(g.apply_to);const a=new w;return a.parse(g.target),{id:c.id,createdAt:c.createdAt,updatedAt:c.updatedAt,data:g,applyTo:v,target:a}}),y.value=[...n.devices],i.value=[...n.networks])},document:L`
    query {
      configs {
        ...ConfigFragment
      }
      devices {
        ...DeviceFragment
      }
      networks {
        ...NetworkFragment
      }
    }
    ${$e}
    ${I}
    ${he}
  `});function D(n){j(be,{id:n.id,name:n.id,gql:L`
      mutation DeleteConfig($id: ID!) {
        deleteConfig(id: $id)
      }
    `,appApi:!1,typeName:"Config"})}function C(n){j(le,{data:n,devices:y,networks:i})}function M(){j(le,{data:null,devices:y,networks:i})}const{mutate:R,loading:q}=B({document:L`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${I}
  `});function O(n){R({id:n.id,input:{group:"route",value:JSON.stringify(n.data)}})}return(n,N)=>{const c=we,g=ke,v=ye("tooltip");return u(),r("div",at,[e("div",ot,[e("div",nt,[V(c,{current:()=>n.$t("page_title.routes")},null,8,["current"]),e("button",{type:"button",class:"btn right-actions",onClick:M},o(n.$t("create")),1)]),e("div",st,[e("table",lt,[e("thead",null,[e("tr",null,[it,e("th",null,o(n.$t("apply_to")),1),e("th",null,o(n.$t("description")),1),e("th",null,o(n.$t("notes")),1),e("th",null,o(n.$t("enabled")),1),e("th",null,o(n.$t("created_at")),1),e("th",null,o(n.$t("updated_at")),1),e("th",dt,o(n.$t("actions")),1)])]),e("tbody",null,[(u(!0),r(b,null,k(_.value,a=>{var T;return u(),r("tr",{key:a.id},[e("td",null,[V(g,{id:a.id,raw:a.data},null,8,["id","raw"])]),e("td",null,o(a.applyTo.getText(n.$t,y.value,i.value)),1),e("td",null,o(n.$t("route_description",{if_name:((T=i.value.find($=>$.ifName==a.data.if_name))==null?void 0:T.name)??a.data.if_name,target:a.target.getText(n.$t,i.value)})),1),e("td",null,o(a.notes),1),e("td",null,[e("div",ut,[e("md-checkbox",{"touch-target":"wrapper",disabled:l(q),onChange:$=>O(a),checked:a.data.is_enabled},null,40,rt)])]),e("td",ct,[f((u(),r("span",null,[oe(o(l(ne)(a.createdAt)),1)])),[[v,l(ae)(a.createdAt)]])]),e("td",pt,[f((u(),r("span",null,[oe(o(l(ne)(a.updatedAt)),1)])),[[v,l(ae)(a.updatedAt)]])]),e("td",_t,[e("a",{href:"#",class:"v-link",onClick:se($=>C(a),["prevent"])},o(n.$t("edit")),9,mt),e("a",{href:"#",class:"v-link",onClick:se($=>D(a),["prevent"])},o(n.$t("delete")),9,vt)])])}),128))])])])])])}}});export{bt as default};
